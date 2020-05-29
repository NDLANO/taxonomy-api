package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.TopicResource;
import no.ndla.taxonomy.service.MetadataWrappedEntity;

import java.net.URI;

@ApiModel("TopicWithResourceConnection")
public class TopicWithResourceConnectionDTO extends TopicDTO {
    @ApiParam
    private URI connectionId;

    @ApiParam
    @JsonProperty("isPrimary")
    private boolean isPrimary;

    @ApiParam
    private int rank;

    public TopicWithResourceConnectionDTO(MetadataWrappedEntity<TopicResource> wrappedTopicResource, String language) {
        this(wrappedTopicResource.getEntity(), language);
        wrappedTopicResource.getMetadata().ifPresent(this::setMetadata);
    }

    public TopicWithResourceConnectionDTO(TopicResource topicResource, String language) {
        super(topicResource.getTopic().orElseThrow(), language);

        this.isPrimary = topicResource.isPrimary().orElse(false);
        this.connectionId = topicResource.getPublicId();
        this.rank = topicResource.getRank();
    }

    public TopicWithResourceConnectionDTO() {
        super();
    }

    public URI getConnectionId() {
        return connectionId;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public int getRank() {
        return rank;
    }
}
