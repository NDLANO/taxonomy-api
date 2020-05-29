package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicTranslation;
import no.ndla.taxonomy.service.MetadataWrappedEntity;

import java.net.URI;
import java.util.Set;

@ApiModel("Topic")
public class TopicDTO {
    @JsonProperty
    @ApiModelProperty(value = "Topic id", example = "urn:topic:234")
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "The name of the topic", example = "Trigonometry")
    public String name;

    @JsonProperty
    @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(value = "The primary path for this topic", example = "/subject:1/topic:1")
    public String path;

    @JsonProperty
    @ApiModelProperty(value = "List of all paths to this topic")
    public Set<String> paths;

    @ApiModelProperty(value = "Metadata object if includeMetadata has been set to true. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public MetadataDto metadata;

    public TopicDTO() {

    }

    public TopicDTO(Topic topic, String languageCode) {
        this.id = topic.getPublicId();
        this.contentUri = topic.getContentUri();
        this.paths = topic.getAllPaths();

        this.path = topic.getPrimaryPath()
                .orElse(null);

        this.name = topic.getTranslation(languageCode)
                .map(TopicTranslation::getName)
                .orElse(topic.getName());
    }

    public TopicDTO(MetadataWrappedEntity<Topic> wrappedTopic, String languageCode) {
        this(wrappedTopic.getEntity(), languageCode);

        wrappedTopic.getMetadata().ifPresent(metadata -> this.metadata = metadata);
    }
}
