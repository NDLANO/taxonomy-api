package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.Resource;
import no.ndla.taxonomy.service.domain.Topic;
import no.ndla.taxonomy.service.domain.TopicResource;
import no.ndla.taxonomy.service.repositories.ResourceRepository;
import no.ndla.taxonomy.service.repositories.TopicRepository;
import no.ndla.taxonomy.service.repositories.TopicResourceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = {"topic-resources", "/v1/topic-resources"})
@Transactional
public class TopicResources {

    private final TopicRepository topicRepository;
    private final ResourceRepository resourceRepository;
    private TopicResourceRepository topicResourceRepository;

    public TopicResources(TopicRepository topicRepository, ResourceRepository resourceRepository, TopicResourceRepository topicResourceRepository) {
        this.topicRepository = topicRepository;
        this.resourceRepository = resourceRepository;
        this.topicResourceRepository = topicResourceRepository;
    }

    @GetMapping
    @ApiOperation(value = "Gets all connections between topics and resources")
    public List<TopicResourceIndexDocument> index() throws Exception {
        List<TopicResourceIndexDocument> result = new ArrayList<>();
        topicResourceRepository.findAll().forEach(record -> result.add(new TopicResourceIndexDocument(record)));
        return result;
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a specific connection between a topic and a resource")
    public TopicResourceIndexDocument get(@PathVariable("id") URI id) throws Exception {
        TopicResource topicResource = topicResourceRepository.getByPublicId(id);
        TopicResourceIndexDocument result = new TopicResourceIndexDocument(topicResource);
        return result;
    }

    @PostMapping
    @ApiOperation(value = "Adds a resource to a topic")
    public ResponseEntity post(
            @ApiParam(name = "connection", value = "new topic/resource connection ") @RequestBody AddResourceToTopicCommand command) throws Exception {

        Topic topic = topicRepository.getByPublicId(command.topicid);
        Resource resource = resourceRepository.getByPublicId(command.resourceid);
        TopicResource topicResource = topic.addResource(resource, command.primary);
        topicResourceRepository.save(topicResource);

        URI location = URI.create("/topic-resources/" + topicResource.getPublicId());
        return ResponseEntity.created(location).build();
    }


    @DeleteMapping("/{id}")
    @ApiOperation("Removes a resource from a topic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) throws Exception {
        topicResourceRepository.getByPublicId(id);
        topicResourceRepository.deleteByPublicId(id);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a connection between a topic and a resource", notes = "Use to update which topic is primary to the resource.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(@PathVariable("id") URI id,
                    @ApiParam(name = "connection", value = "Updated topic/resource connection") @RequestBody UpdateTopicResourceCommand
                            command) throws Exception {
        TopicResource topicResource = topicResourceRepository.getByPublicId(id);
        topicResource.setPrimary(command.primary);
    }

    public static class AddResourceToTopicCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic id", example = "urn:topic:345")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource id", example = "urn:resource:345")
        URI resourceid;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean primary;
    }

    public static class UpdateTopicResourceCommand {
        @JsonProperty
        @ApiModelProperty(value = "Topic resource connection id", example = "urn:topic-has-resources:123")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean primary;
    }

    public static class TopicResourceIndexDocument {

        @JsonProperty
        @ApiModelProperty(value = "Topic id", example = "urn:topic:345")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(value = "Resource id", example = "urn:resource:345")
        URI resourceid;

        @JsonProperty
        @ApiModelProperty(value = "Topic resource connection id", example = "urn:topic-has-resources:123")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean primary;

        TopicResourceIndexDocument() {
        }

        TopicResourceIndexDocument(TopicResource topicResource) {
            id = topicResource.getPublicId();
            topicid = topicResource.getTopic().getPublicId();
            resourceid = topicResource.getResource().getPublicId();
            primary = topicResource.isPrimary();
        }
    }
}
