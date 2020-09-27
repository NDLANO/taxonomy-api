package no.ndla.taxonomy.service.dtos;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicResource;

import java.net.URI;

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

    public ResourceWithTopicConnectionDTO(TopicResource topicResource, String language) {
        super(topicResource.getResource().orElseThrow(), language);

        this.topicId = topicResource.getTopic()
                .map(Topic::getPublicId)
                .orElse(null);

        this.connectionId = topicResource.getPublicId();
        this.rank = topicResource.getRank();
        this.primary = topicResource.isPrimary().orElse(false);
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

    public boolean isPrimary() {
        return primary;
    }
}
