package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.TopicSubtopic;
import no.ndla.taxonomy.domain.TopicTranslation;
import no.ndla.taxonomy.service.MetadataWrappedEntity;

import java.net.URI;

/**
 *
 */
@ApiModel("SubTopicIndexDocument")
public class SubTopicIndexDTO {
    @JsonProperty
    @ApiModelProperty(value = "Topic id", example = "urn:topic:234")
    private URI id;

    @JsonProperty
    @ApiModelProperty(value = "The name of the subtopic", example = "Trigonometry")
    private String name;

    @JsonProperty
    @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    private URI contentUri;

    @JsonProperty
    @ApiModelProperty(value = "True if owned by this topic, false if it has its primary connection elsewhere", example = "true")
    private Boolean isPrimary;

    @ApiModelProperty(value = "Metadata object if includeMetadata has been set to true. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataDto metadata;

    public SubTopicIndexDTO() {

    }

    public SubTopicIndexDTO(MetadataWrappedEntity<TopicSubtopic> wrappedTopicSubtopic, String language) {
        final var topicSubtopic = wrappedTopicSubtopic.getEntity();

        topicSubtopic.getSubtopic().ifPresent(topic -> {
            this.id = topic.getPublicId();

            this.name = topic.getTranslation(language)
                    .map(TopicTranslation::getName)
                    .orElse(topic.getName());

            this.contentUri = topic.getContentUri();
        });

        this.isPrimary = true;

        wrappedTopicSubtopic.getMetadata().ifPresent(metadataDto -> this.metadata = metadataDto);
    }

    public URI getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public URI getContentUri() {
        return contentUri;
    }

    public Boolean getPrimary() {
        return isPrimary;
    }

    public MetadataDto getMetadata() {
        return metadata;
    }
}
