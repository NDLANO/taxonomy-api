package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.repositories.TopicRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static no.ndla.taxonomy.service.jdbc.QueryUtils.*;
import static no.ndla.taxonomy.service.rest.v1.DocStrings.LANGUAGE_DOC;

@RestController
@RequestMapping(path = {"topics", "/v1/topics"})
@Transactional
public class Topics {

    private TopicRepository topicRepository;
    private JdbcTemplate jdbcTemplate;

    private static final String getTopicsQuery = getQuery("get_topics");
    private static final String resourceQueryRecursive = getQuery("get_resources_by_topic_public_id_recursively");
    private static final String resourceQuery = getQuery("get_resources_by_topic_public_id");

    public Topics(TopicRepository topicRepository, JdbcTemplate jdbcTemplate) {
        this.topicRepository = topicRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    @ApiOperation("Gets all topics")
    public List<TopicIndexDocument> index(
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) throws Exception {
        List<Object> args = asList(language);
        return getTopicIndexDocuments(getTopicsQuery, args);
    }

    @GetMapping("/{id}")
    @ApiOperation("Gets a single topic")
    public TopicIndexDocument get(@PathVariable("id") URI id,
                                  @ApiParam(value = LANGUAGE_DOC, example = "nb")
                                  @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) throws Exception {

        String sql = getTopicsQuery.replace("1 = 1", "t.public_id = ?");
        List<Object> args = asList(language, id.toString());

        return getFirst(getTopicIndexDocuments(sql, args), "Topic", id);
    }

    private List<TopicIndexDocument> getTopicIndexDocuments(String sql, List<Object> args) {
        return jdbcTemplate.query(sql, setQueryParameters(args),
                resultSet -> {
                    List<TopicIndexDocument> result = new ArrayList<>();
                    while (resultSet.next()) {
                        result.add(new TopicIndexDocument() {{
                            name = resultSet.getString("topic_name");
                            id = getURI(resultSet, "topic_public_id");
                            contentUri = getURI(resultSet, "topic_content_uri");
                        }});
                    }
                    return result;
                }
        );
    }

    @PostMapping
    @ApiOperation(value = "Creates a new topic")
    public ResponseEntity<Void> post(@ApiParam(name = "topic", value = "The new topic") @RequestBody CreateTopicCommand command) throws Exception {
        try {
            Topic topic = new Topic();
            if (null != command.id) topic.setPublicId(command.id);
            topic.name(command.name);
            topic.setContentUri(command.contentUri);
            URI location = URI.create("/topics/" + topic.getPublicId());
            topicRepository.save(topic);
            return ResponseEntity.created(location).build();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateIdException(command.id.toString());
        }
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a single topic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) throws Exception {
        topicRepository.getByPublicId(id);
        topicRepository.deleteByPublicId(id);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a single topic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "topic", value = "The updated topic") @RequestBody UpdateTopicCommand command) throws Exception {
        Topic topic = topicRepository.getByPublicId(id);
        topic.setName(command.name);
        topic.setContentUri(command.contentUri);
    }

    @PutMapping
    @ApiOperation(value = "Replaces the collection of topics.")
    public void putCollection(@ApiParam(name = "subjects", value = "A list of subjects") @RequestBody CreateTopicCommand[] commands) throws Exception {
        topicRepository.deleteAll();
        for (CreateTopicCommand command : commands) {
            post(command);
        }
    }

    @GetMapping("/{id}/resources")
    @ApiOperation(value = "Gets all resources for the given topic")
    public List<ResourceIndexDocument> getResources(
            @PathVariable("id") URI topicId,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language,
            @RequestParam(value = "recursive", required = false, defaultValue = "false")
            @ApiParam("If true, resources from subtopics are fetched recursively")
                    boolean recursive,
            @RequestParam(value = "type", required = false, defaultValue = "")
            @ApiParam(value = "Filter by resource type id(s). If not specified, resources of all types will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] resourceTypeIds
    ) throws SQLException {

        List<Object> args = new ArrayList<>();
        String query;
        if (recursive) {
            query = resourceQueryRecursive;
            args.add(topicId.toString());
            args.add(language);
            args.add(language);
        } else {
            query = resourceQuery;
            args.add(language);
            args.add(language);
            args.add(topicId.toString());
        }

        if (resourceTypeIds.length > 0) {
            StringBuilder where = new StringBuilder();
            for (URI resourceTypeId : resourceTypeIds) {
                where.append("rt.public_id = ? OR ");
                args.add(resourceTypeId.toString());
            }
            where.setLength(where.length() - 4);
            query = query.replace("1 = 1", "(" + where + ")");
        }

        return jdbcTemplate.query(query, setQueryParameters(args), resultSet -> {
            List<ResourceIndexDocument> result = new ArrayList<>();
            ResourceIndexDocument current, previous = null;

            while (resultSet.next()) {
                URI id = toURI(resultSet.getString("resource_public_id"));

                boolean duplicate = previous != null && id.equals(previous.id);
                if (duplicate) {
                    current = previous;
                } else {
                    current = new ResourceIndexDocument() {{
                        topicId = toURI(resultSet.getString("topic_id"));
                        name = resultSet.getString("resource_name");
                        contentUri = toURI(resultSet.getString("resource_content_uri"));
                        id = toURI(resultSet.getString("resource_public_id"));
                    }};
                    result.add(current);
                }

                String resourceTypePublicId = resultSet.getString("resource_type_public_id");
                if (resourceTypePublicId != null) {
                    ResourceTypeIndexDocument resourceType = new ResourceTypeIndexDocument() {{
                        id = toURI(resourceTypePublicId);
                        name = resultSet.getString("resource_type_name");
                    }};

                    current.resourceTypes.add(resourceType);
                }
                previous = current;
            }

            return result;
        });
    }

    public static class ResourceIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Topic id", example = "urn:topic:123")
        public URI topicId;

        @JsonProperty
        @ApiModelProperty(value = "Resource id", example = "urn:resource:12")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Resource name", example = "Basic physics")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "Resource type(s)", example = "[{id = 'urn:resource-type:1', name = 'lecture'}]")
        public List<ResourceTypeIndexDocument> resourceTypes = new ArrayList<>();

        @JsonProperty
        @ApiModelProperty(notes = "The ID of this resource in the system where the content is stored. " +
                "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                "for the system, and <id> is the id of this content in that system.", example = "urn:article:1")
        public URI contentUri;
    }

    public static class ResourceTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Resource type id", example = "urn:resource-type:12")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Resource type name", example = "Assignment")
        public String name;
    }

    public static class CreateTopicCommand {
        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:topic: and be a valid URI. If omitted, an id will be assigned automatically.", example = "urn:topic:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(notes = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the topic", example = "Trigonometry")
        public String name;
    }

    public static class UpdateTopicCommand {
        @JsonProperty
        @ApiModelProperty(notes = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the topic", example = "Trigonometry")
        public String name;
    }

    public static class TopicIndexDocument {
        @JsonProperty
        @ApiModelProperty(value = "Topic id", example = "urn:topic:234")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the topic", example = "Trigonometry")
        public String name;

        @JsonProperty
        @ApiModelProperty(notes = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
        public URI contentUri;

        TopicIndexDocument() {
        }
    }
}