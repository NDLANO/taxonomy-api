package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.repositories.TopicRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@RestController
@RequestMapping(path = {"topics", "/v1/topics"})
@Transactional
public class Topics {

    private TopicRepository topicRepository;
    private JdbcTemplate jdbcTemplate;

    public Topics(TopicRepository topicRepository, JdbcTemplate jdbcTemplate) {
        this.topicRepository = topicRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    @ApiOperation("Gets all topics")
    public List<TopicIndexDocument> index() throws Exception {
        List<TopicIndexDocument> result = new ArrayList<>();
        Iterable<Topic> all = topicRepository.findAll();
        all.forEach(topic -> result.add(new TopicIndexDocument(topic)));
        return result;
    }

    @GetMapping("/{id}")
    @ApiOperation("Gets a single topic")
    public TopicIndexDocument get(@PathVariable("id") URI id) throws Exception {
        Topic topic = topicRepository.getByPublicId(id);
        TopicIndexDocument result = new TopicIndexDocument(topic);
        return result;
    }

    @PostMapping
    @ApiOperation(value = "Creates a new topic")
    public ResponseEntity<Void> post(@ApiParam(name = "topic", value = "The new topic") @RequestBody CreateTopicCommand command) throws Exception {
        try {
            Topic topic = new Topic();
            if (null != command.id) topic.setPublicId(command.id);
            topic.name(command.name);
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
    }

    @PutMapping
    @ApiOperation(value = "Replaces the collection of topics.")
    public void putCollection(@ApiParam(name = "subjects", value = "A list of subjects") @RequestBody CreateTopicCommand[] commands) throws Exception {
        topicRepository.deleteAll();
        for (CreateTopicCommand command : commands) {
            post(command);
        }
    }

    public List<ResourceIndexDocument> getResources(@PathVariable("id") URI topicId) {

        String query = getQuery("get_resources_by_topic_public_id_recursively.sql");

        List<ResourceIndexDocument> results = jdbcTemplate.query(
                query,
                (resultSet, i) -> new ResourceIndexDocument() {{
                    topicId = URI.create(resultSet.getString("topic_id"));
                }},
                topicId
        );

        return results;
    }

    public static String getQuery(String name) {
        try (
                InputStream inputStream = new ClassPathResource("db.queries." + name).getInputStream()
        ) {
            return new Scanner(inputStream).useDelimiter("\\Z").next();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ResourceIndexDocument {
        public URI topicId, resourceId;
        public String resourceName;
    }

    public static class CreateTopicCommand {
        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:topic: and be a valid URI. If omitted, an id will be assigned automatically.", example = "urn:topic:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the topic", example = "Trigonometry")
        public String name;
    }

    public static class UpdateTopicCommand {
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

        TopicIndexDocument() {
        }

        TopicIndexDocument(Topic topic) {
            id = topic.getPublicId();
            name = topic.getName();
        }
    }
}
