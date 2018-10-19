package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.rest.v1.command.topics.CreateTopicCommand;
import no.ndla.taxonomy.rest.v1.command.topics.UpdateTopicCommand;
import no.ndla.taxonomy.rest.v1.dto.topics.*;
import no.ndla.taxonomy.rest.v1.extractors.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.ndla.taxonomy.jdbc.QueryUtils.*;
import static no.ndla.taxonomy.rest.v1.DocStrings.LANGUAGE_DOC;

@RestController
@RequestMapping(path = {"/v1/topics"})
@Transactional
public class Topics extends CrudController<Topic> {

    private JdbcTemplate jdbcTemplate;

    private static final String GET_TOPICS_QUERY = getQuery("get_topics");
    private static final String GET_RESOURCES_BY_TOPIC_PUBLIC_ID_RECURSIVELY_QUERY = getQuery("get_resources_by_topic_public_id_recursively");
    private static final String GET_RESOURCES_BY_TOPIC_PUBLIC_ID_QUERY = getQuery("get_resources_by_topic_public_id");
    private static final String GET_FILTERS_BY_TOPIC_ID_QUERY = getQuery("get_filters_by_topic_public_id");
    private static final String GET_SUBTOPICS_BY_TOPIC_ID_QUERY = getQuery("get_subtopics_by_topic_id_query");
    private static final String GET_SUBJECT_CONNECTIONS_BY_TOPIC_ID_QUERY = getQuery("get_subject_connections_by_topic_id");
    private static final String GET_SUBTOPIC_CONNECTIONS_BY_TOPIC_ID_QUERY = getQuery("get_subtopic_connections_by_topic_id");
    private static final String GET_PARENT_TOPIC_CONNECTIONS_BY_TOPIC_ID_QUERY = getQuery("get_parent_topic_connections_by_topic_id");


    public Topics(TopicRepository topicRepository, JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        repository = topicRepository;
    }


    @GetMapping
    @ApiOperation("Gets all topics")
    @PreAuthorize("hasAuthority('READONLY')")
    public List<TopicIndexDocument> index(
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) throws Exception {
        List<Object> args = asList(language);
        TopicQueryExtractor extractor = new TopicQueryExtractor();
        return jdbcTemplate.query(GET_TOPICS_QUERY, setQueryParameters(args),
                extractor::extractTopics
        );
    }


    @GetMapping("/{id}")
    @ApiOperation("Gets a single topic")
    @PreAuthorize("hasAuthority('READONLY')")
    public TopicIndexDocument get(@PathVariable("id") URI id,
                                  @ApiParam(value = LANGUAGE_DOC, example = "nb")
                                  @RequestParam(value = "language", required = false, defaultValue = "") String language
    ) {
        String sql = GET_TOPICS_QUERY.replace("1 = 1", "t.public_id = ?");
        List<Object> args = asList(language, id.toString());

        TopicQueryExtractor extractor = new TopicQueryExtractor();
        return getFirst(jdbcTemplate.query(sql, setQueryParameters(args),
                extractor::extractTopics
        ), "Topic", id);
    }


    @PostMapping
    @ApiOperation(value = "Creates a new topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(@ApiParam(name = "connection", value = "The new topic") @RequestBody CreateTopicCommand command) {
        return doPost(new Topic(), command);
    }


    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a single topic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "topic", value = "The updated topic. Fields not included will be set to null.") @RequestBody UpdateTopicCommand command) {
        doPut(id, command);
    }


    @GetMapping("/{id}/resources")
    @PreAuthorize("hasAuthority('READONLY')")
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
            @ApiParam(value = "Select by resource type id(s). If not specified, resources of all types will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] resourceTypeIds,
            @RequestParam(value = "filter", required = false, defaultValue = "")
            @ApiParam(value = "Select by filter id(s). If not specified, all resources will be returned." +
                    "Multiple ids may be separated with comma or the parameter may be repeated for each id.", allowMultiple = true)
                    URI[] filterIds,
            @RequestParam(value = "relevance", required = false, defaultValue = "")
            @ApiParam(value = "Select by relevance. If not specified, all resources will be returned.")
                    URI relevance
    ) throws Exception {

        TopicIndexDocument topicIndexDocument = get(topicId, null);

        List<Object> args = new ArrayList<>();
        String query;
        if (recursive) {
            query = GET_RESOURCES_BY_TOPIC_PUBLIC_ID_RECURSIVELY_QUERY;
            args.add(topicId.toString());
            args.add(language);
            args.add(language);
        } else {
            query = GET_RESOURCES_BY_TOPIC_PUBLIC_ID_QUERY;
            args.add(language);
            args.add(language);
            args.add(topicId.toString());
        }
        ResourceQueryExtractor extractor = new ResourceQueryExtractor();
        query = extractor.addResourceTypesToQuery(resourceTypeIds, args, query);
        query = extractor.addFiltersToQuery(filterIds, args, query);

        return jdbcTemplate.query(query, setQueryParameters(args), resultSet -> {
            return extractor.extractResources(relevance, topicIndexDocument, resultSet);
        });
    }


    @GetMapping("/{id}/filters")
    @PreAuthorize("hasAuthority('READONLY')")
    @ApiOperation(value = "Gets all filters associated with this topic")
    public List<FilterIndexDocument> getFilters(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        FilterQueryExtractor extractor = new FilterQueryExtractor();
        return jdbcTemplate.query(GET_FILTERS_BY_TOPIC_ID_QUERY, setQueryParameters(singletonList(id.toString())),
                extractor::extractFilters
        );
    }

    @GetMapping("/{id}/topics")
    @PreAuthorize("hasAuthority('READONLY')")
    @ApiOperation(value = "Gets all subtopics for this topic")
    public List<SubTopicIndexDocument> getSubTopics(
            @PathVariable("id")
                    URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        String sql = GET_SUBTOPICS_BY_TOPIC_ID_QUERY.replace("1 = 1", "t.public_id = ?");
        List<Object> args = asList(language, id.toString());

        SubTopicQueryExtractor extractor = new SubTopicQueryExtractor();
        return jdbcTemplate.query(sql, setQueryParameters(args),
                extractor::extractSubTopics
        );
    }

    @GetMapping("/{id}/connections")
    @PreAuthorize("hasAuthority('READONLY')")
    @ApiOperation(value = "Gets all subjects and subtopics this topic is connected to")
    public List<ConnectionIndexDocument> getAllConnections(@PathVariable("id") URI id) {
        List<Object> args = asList(id.toString());
        List<ConnectionIndexDocument> results = new ArrayList<>();
        ConnectionQueryExtractor ConnectionQueryExtractor = new ConnectionQueryExtractor();
        results.addAll(jdbcTemplate.query(GET_PARENT_TOPIC_CONNECTIONS_BY_TOPIC_ID_QUERY, setQueryParameters(args), ConnectionQueryExtractor::extractConnections));
        results.addAll(jdbcTemplate.query(GET_SUBJECT_CONNECTIONS_BY_TOPIC_ID_QUERY, setQueryParameters(args), ConnectionQueryExtractor::extractConnections));
        results.addAll(jdbcTemplate.query(GET_SUBTOPIC_CONNECTIONS_BY_TOPIC_ID_QUERY, setQueryParameters(args), ConnectionQueryExtractor::extractConnections));
        return results;
    }

}
