package no.ndla.taxonomy.rest.v1.dtos.subjects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.TopicTranslation;
import no.ndla.taxonomy.service.MetadataIdField;
import no.ndla.taxonomy.service.dtos.MetadataDto;

import java.net.URI;

/**
 *
 */
@ApiModel("SubjectIndexDocument")
public class SubjectIndexDocument {
    @JsonProperty
    @ApiModelProperty(example = "urn:subject:1")
    @MetadataIdField
    public URI id;

    @JsonProperty
    @ApiModelProperty(value = "ID of article introducing this subject. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty(value = "The name of the subject", example = "Mathematics")
    public String name;

    @JsonProperty
    @ApiModelProperty(value = "The path part of the url to this subject.", example = "/subject:1")
    public String path;

    @ApiModelProperty(value = "Metadata for entity. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataDto metadata;

    public SubjectIndexDocument() {

    }

    public MetadataDto getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataDto metadata) {
        this.metadata = metadata;
    }

    public SubjectIndexDocument(Topic topic, String languageCode) {
        this.id = topic.getPublicId();
        this.contentUri = topic.getContentUri();
        this.name = topic
                .getTranslation(languageCode)
                .map(TopicTranslation::getName)
                .orElse(topic.getName());
        this.path = topic
                .getPrimaryPath()
                .orElse(null);
    }
}
