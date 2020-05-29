package no.ndla.taxonomy.service.dtos;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResource;
import no.ndla.taxonomy.service.MetadataWrappedEntity;

import java.net.URI;

@ApiModel("ResourceWithTopicConnection")
public class ResourceWithTopicConnectionDTO extends ResourceDTO {
    @ApiParam
    public URI topicId;

    @ApiParam
    public URI connectionId;

    @ApiParam
    public int rank;

    public ResourceWithTopicConnectionDTO() {

    }

    public ResourceWithTopicConnectionDTO(MetadataWrappedEntity<TopicResource> wrappedTopicResource, String language) {
        this(wrappedTopicResource.getEntity(), language);
        wrappedTopicResource.getMetadata().ifPresent(metadataDto -> this.metadata = metadataDto);
    }

    public ResourceWithTopicConnectionDTO(TopicResource topicResource, String language) {
        super(topicResource.getResource().orElseThrow(), language);

        this.topicId = topicResource.getTopic()
                .map(Topic::getPublicId)
                .orElse(null);

        this.connectionId = topicResource.getPublicId();
        this.rank = topicResource.getRank();
    }
}
