package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Resource;

import java.util.HashSet;
import java.util.Set;

@ApiModel("ResourceWithParentTopics")
public class ResourceWithParentTopicsDTO extends ResourceDTO {
    @JsonProperty
    @ApiModelProperty(value = "Parent topology nodes and whether or not connection type is primary",
            example = "[" +
                    "{\"id\": \"urn:topic:1:181900\"," +
                    "\"name\": \"I dyrehagen\"," +
                    "\"contentUri\": \"urn:article:6662\"," +
                    "\"path\": \"/subject:2/topic:1:181900\"," +
                    "\"primary\": \"true\"}]")
    private Set<TopicWithResourceConnectionDTO> parentTopics = new HashSet<>();

    public ResourceWithParentTopicsDTO() {
        super();
    }

    public ResourceWithParentTopicsDTO(Resource resource, String languageCode) {
        super(resource, languageCode);

        resource.getTopicResources().stream()
                .map(topicResource -> new TopicWithResourceConnectionDTO(topicResource, languageCode))
                .forEach(parentTopics::add);
    }

    public Set<TopicWithResourceConnectionDTO> getParentTopics() {
        return parentTopics;
    }
}
