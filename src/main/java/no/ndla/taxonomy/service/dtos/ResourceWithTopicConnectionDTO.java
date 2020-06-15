package no.ndla.taxonomy.service.dtos;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResource;
import no.ndla.taxonomy.service.MetadataWrappedEntity;

import java.net.URI;
import java.util.Optional;

@ApiModel("ResourceWithTopicConnection")
public class ResourceWithTopicConnectionDTO extends ResourceDTO {
    @ApiParam
    private URI topicId;

    @ApiParam
    private URI connectionId;

    @ApiParam
    private int rank;

    @ApiParam
    private boolean primary;

    public ResourceWithTopicConnectionDTO() {

    }

    public ResourceWithTopicConnectionDTO(MetadataWrappedEntity<TopicResource> wrappedTopicResource, String language) {
        this(wrappedTopicResource.getEntity(), language);

        wrappedTopicResource.getMetadata().ifPresent(this::setMetadata);
    }

    public ResourceWithTopicConnectionDTO(TopicResource topicResource, String language) {
        super(topicResource.getResource().orElseThrow(), language);

        this.topicId = topicResource.getTopic()
                .map(Topic::getPublicId)
                .orElse(null);

        this.connectionId = topicResource.getPublicId();
        this.rank = topicResource.getRank();
        this.primary = topicResource.isPrimary().get();
    }

    public URI getTopicId() {
        return topicId;
    }

    public URI getConnectionId() {
        return connectionId;
    }

    public int getRank() {
        return rank;
    }
}
