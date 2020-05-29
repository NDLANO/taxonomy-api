package no.ndla.taxonomy.service.dtos;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.TopicResource;
import no.ndla.taxonomy.service.MetadataWrappedEntity;

import java.net.URI;

@ApiModel("TopicWithResourceConnection")
public class TopicWithResourceConnectionDTO extends TopicDTO {
    @ApiParam
    public URI connectionId;

    @ApiParam
    public boolean isPrimary;

    @ApiParam
    public int rank;

    public TopicWithResourceConnectionDTO(MetadataWrappedEntity<TopicResource> wrappedTopicResource, String language) {
        this(wrappedTopicResource.getEntity(), language);
        wrappedTopicResource.getMetadata().ifPresent(metadataDto -> this.metadata = metadataDto);
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
}
