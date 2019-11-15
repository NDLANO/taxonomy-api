package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.PrimaryParentRequiredException;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResource;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.TopicResourceRepository;
import no.ndla.taxonomy.rest.BadHttpRequestException;
import no.ndla.taxonomy.rest.ConflictHttpResponseException;
import no.ndla.taxonomy.rest.NotFoundHttpRequestException;
import no.ndla.taxonomy.service.EntityConnectionService;
import no.ndla.taxonomy.service.exceptions.DuplicateConnectionException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = {"/v1/topic-resources"})
@Transactional
public class TopicResources {

    private final TopicRepository topicRepository;
    private final ResourceRepository resourceRepository;
    private final TopicResourceRepository topicResourceRepository;
    private final EntityConnectionService connectionService;

    public TopicResources(TopicRepository topicRepository,
                          ResourceRepository resourceRepository,
                          TopicResourceRepository topicResourceRepository,
                          EntityConnectionService connectionService) {
        this.topicRepository = topicRepository;
        this.resourceRepository = resourceRepository;
        this.topicResourceRepository = topicResourceRepository;
        this.connectionService = connectionService;
    }

    @GetMapping
    @ApiOperation(value = "Gets all connections between topics and resources")
    public List<TopicResourceIndexDocument> index() {
        return topicResourceRepository
                .findAllIncludingTopicAndResource()
                .stream()
                .map(TopicResourceIndexDocument::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a specific connection between a topic and a resource")
    public TopicResourceIndexDocument get(@PathVariable("id") URI id) {
        TopicResource topicResource = topicResourceRepository.getByPublicId(id);
        return new TopicResourceIndexDocument(topicResource);
    }

    @PostMapping
    @ApiOperation(value = "Adds a resource to a topic")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(
            @ApiParam(name = "connection", value = "new topic/resource connection ") @RequestBody AddResourceToTopicCommand command) {

        Topic topic = topicRepository.getByPublicId(command.topicid);
        Resource resource = resourceRepository.getByPublicId(command.resourceId);

        final TopicResource topicResource;
        try {
            topicResource = connectionService.connectTopicResource(topic, resource, command.primary, command.rank == 0 ? null : command.rank);
        } catch (DuplicateConnectionException e) {
            throw new ConflictHttpResponseException(e);
        } catch (InvalidArgumentServiceException e) {
            throw new BadHttpRequestException(e);
        }

        URI location = URI.create("/topic-resources/" + topicResource.getPublicId());
        return ResponseEntity.created(location).build();
    }


    @DeleteMapping("/{id}")
    @ApiOperation("Removes a resource from a topic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void delete(@PathVariable("id") URI id) {
        connectionService.disconnectTopicResource(topicResourceRepository.getByPublicId(id));
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Updates a connection between a topic and a resource", notes = "Use to update which topic is primary to the resource or to change sorting order.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(@PathVariable("id") URI id,
                    @ApiParam(name = "connection", value = "Updated topic/resource connection") @RequestBody UpdateTopicResourceCommand
                            command) {
        TopicResource topicResource = topicResourceRepository.getByPublicId(id);

        if (topicResource.isPrimary().orElseThrow() && !command.primary) {
            throw new PrimaryParentRequiredException();
        }

        try {
            connectionService.updateTopicResource(topicResource, command.primary, command.rank > 0 ? command.rank : null);
        } catch (InvalidArgumentServiceException e) {
            throw new BadHttpRequestException(e);
        } catch (NotFoundServiceException e) {
            throw new NotFoundHttpRequestException(e);
        }
    }

    public static class AddResourceToTopicCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "Topic id", example = "urn:topic:345")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(required = true, value = "Resource id", example = "urn:resource:345")
        public URI resourceId;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean primary = true;

        @JsonProperty
        @ApiModelProperty(value = "Order in which resource is sorted for the topic", example = "1")
        public int rank;
    }

    public static class UpdateTopicResourceCommand {
        @JsonProperty
        @ApiModelProperty(value = "Topic resource connection id", example = "urn:topic-has-resources:123")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which the resource will be sorted for this topic.", example = "1")
        public int rank;
    }

    public static class TopicResourceIndexDocument {

        @JsonProperty
        @ApiModelProperty(value = "Topic id", example = "urn:topic:345")
        public URI topicid;

        @JsonProperty
        @ApiModelProperty(value = "Resource id", example = "urn:resource:345")
        URI resourceId;

        @JsonProperty
        @ApiModelProperty(value = "Topic resource connection id", example = "urn:topic-has-resources:123")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "Primary connection", example = "true")
        public boolean primary;

        @JsonProperty
        @ApiModelProperty(value = "Order in which the resource is sorted for the topic", example = "1")
        public int rank;

        TopicResourceIndexDocument() {
        }

        TopicResourceIndexDocument(TopicResource topicResource) {
            id = topicResource.getPublicId();
            topicResource.getTopic().ifPresent(topic -> topicid = topic.getPublicId());
            topicResource.getResource().ifPresent(resource -> resourceId = resource.getPublicId());
            primary = topicResource.isPrimary().orElseThrow();
            rank = topicResource.getRank();
        }
    }
}
