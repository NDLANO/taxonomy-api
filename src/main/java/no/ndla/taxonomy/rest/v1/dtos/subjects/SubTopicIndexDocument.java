package no.ndla.taxonomy.rest.v1.dtos.subjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.service.InjectMetadata;
import no.ndla.taxonomy.service.MetadataIdField;
import no.ndla.taxonomy.service.TopicTreeSorter;
import no.ndla.taxonomy.service.dtos.MetadataDto;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
@ApiModel("SubTopicIndexDocument")
public class SubTopicIndexDocument implements TopicTreeSorter.Sortable {
    @JsonProperty
    @MetadataIdField
    public URI id;

    @JsonProperty
    public String name;

    @JsonProperty
    @ApiModelProperty(value = "ID of article introducing this topic. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @ApiModelProperty("Parent id in the current context, null if none exists")
    public URI parent;

    @JsonProperty
    @ApiModelProperty(value = "The primary path to this topic.", example = "/subject:1/topic:1")
    public String path;

    @JsonProperty
    @ApiModelProperty(value = "The id of the subject-topics or topic-subtopics connection which causes this topic to be included in the result set.", example = "urn:subject-topic:1")
    public URI connectionId;

    @JsonProperty
    @ApiModelProperty(value = "Primary connection", example = "true")
    public boolean isPrimary;

    @JsonProperty
    @ApiModelProperty(value = "The order in which to sort the topic within it's level.", example = "1")
    public int rank;

    @JsonProperty
    @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
    public URI relevanceId;

    @JsonProperty
    @ApiModelProperty(value = "Filters this topic is associated with, directly or by inheritance", example = "[{\"id\":\"urn:filter:1\", \"relevanceId\":\"urn:relevance:core\"}]")
    @InjectMetadata
    public Set<Object> filters = new HashSet<>();

    @JsonIgnore
    public URI topicFilterId, filterPublicId;

    @JsonIgnore
    public List<SubTopicIndexDocument> children = new ArrayList<>();

    private String language;

    @ApiModelProperty(value = "Metadata for entity. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataDto metadata;

    public SubTopicIndexDocument(Subject subject, DomainEntity domainEntity, String language) {
        this.language = language;

        if (domainEntity instanceof TopicSubtopic) {
            final var topicSubtopic = (TopicSubtopic) domainEntity;

            topicSubtopic.getSubtopic().ifPresent(subtopic -> {
                this.populateFromTopic(subtopic);
                this.path = subtopic.getPathByContext(subject).orElse(null);
            });

            topicSubtopic.getTopic().ifPresent(topic -> this.parent = topic.getPublicId());

            this.rank = topicSubtopic.getRank();

            {
                final Relevance relevance = topicSubtopic.getRelevance().orElse(null);
                this.relevanceId = relevance != null ? relevance.getPublicId() : null;
            }
        } else if (domainEntity instanceof SubjectTopic) {
            final var subjectTopic = (SubjectTopic) domainEntity;

            subjectTopic.getTopic().ifPresent(topic -> {
                this.populateFromTopic(topic);
                this.path = topic.getPathByContext(subject).orElse(null);
            });

            subjectTopic.getSubject().ifPresent(s -> this.parent = s.getPublicId());

            this.rank = subjectTopic.getRank();

            {
                final Relevance relevance = subjectTopic.getRelevance().orElse(null);
                this.relevanceId = relevance != null ? relevance.getPublicId() : null;
            }
        } else {
            throw new IllegalArgumentException("Wrapped entity must be either a SubjectTopic or TopicSubtopic");
        }

        this.connectionId = domainEntity.getPublicId();

        this.isPrimary = true;
    }

    public MetadataDto getMetadata() {
        return metadata;
    }

    @Override
    @JsonIgnore
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubTopicIndexDocument)) return false;

        SubTopicIndexDocument that = (SubTopicIndexDocument) o;

        return id.equals(that.id);
    }

    @Override
    @JsonIgnore
    public int hashCode() {
        return id.hashCode();
    }

    public SubTopicIndexDocument() {

    }

    public void setMetadata(MetadataDto metadata) {
        this.metadata = metadata;
    }

    private void populateFromTopic(Topic topic) {
        this.id = topic.getPublicId();
        this.name = topic.getTranslation(this.language)
                .map(TopicTranslation::getName)
                .orElse(topic.getName());
        this.contentUri = topic.getContentUri();
    }

    @Override
    public int getSortableRank() {
        return rank;
    }

    @Override
    public URI getSortableId() {
        return id;
    }

    @Override
    public URI getSortableParentId() {
        return parent;
    }
}
