package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.TopicResourceType;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.service.TopicResourceTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = {"/v1/topic-resourcetypes"})
@Transactional
public class TopicsWithResourceTypes {
    private final TopicResourceTypeService topicResourceTypeService;

    public TopicsWithResourceTypes(TopicResourceTypeService topicResourceTypeService) {
        this.topicResourceTypeService = topicResourceTypeService;
    }

    @PostMapping
    @ApiOperation(value = "Adds a resource type to a topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "connection", value = "The new resource/resource type connection") @RequestBody CreateTopicResourceTypeCommand command) {

        URI location = URI.create("/topic-resourcetypes/" + topicResourceTypeService.addTopicResourceType(command.topicId, command.resourceTypeId));
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping({"/{id}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation("Removes a resource type from a topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) {
        topicResourceTypeService.deleteTopicResourceType(id);
    }

    @GetMapping
    @ApiOperation("Gets all connections between topics and resource types")
    public List<TopicResourceTypeIndexDocument> index() {
        return topicResourceTypeService.findAll()
                .map(TopicResourceTypeIndexDocument::new)
                .collect(Collectors.toList());
    }

    @GetMapping({"/{id}"})
    @ApiOperation("Gets a single connection between topic and resource type")
    public TopicResourceTypeIndexDocument get(@PathVariable("id") URI id) {
        return new TopicResourceTypeIndexDocument(
                topicResourceTypeService.findById(id).orElseThrow(() -> new NotFoundHttpResponseException("TopicResourceType not found"))
        );
    }

    public static class CreateTopicResourceTypeCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic id", example = "urn:topic:123")
        URI topicId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource type id", example = "urn:resourcetype:234")
        URI resourceTypeId;
    }

    @ApiModel("ResourceTypeIndexDocument")
    public static class TopicResourceTypeIndexDocument {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic type id", example = "urn:topic:123")
        URI topicId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource type id", example = "urn:resourcetype:234")
        URI resourceTypeId;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource to resource type connection id", example = "urn:resource-has-resourcetypes:12")
        URI id;

        public TopicResourceTypeIndexDocument() {
        }

        public TopicResourceTypeIndexDocument(TopicResourceType resourceResourceType) {
            id = resourceResourceType.getPublicId();
            resourceResourceType.getResourceType().ifPresent(resourceType -> resourceTypeId = resourceType.getPublicId());
            resourceResourceType.getTopic().ifPresent(topic -> topicId = topic.getPublicId());
        }
    }
}
