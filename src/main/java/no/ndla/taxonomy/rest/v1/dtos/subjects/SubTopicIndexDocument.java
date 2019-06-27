package no.ndla.taxonomy.rest.v1.dtos.subjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.service.TopicTreeSorter;

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
    @ApiModelProperty(value = "Filters this topic is associated with, directly or by inheritance", example = "[{\"id\":\"urn:filter:1\", \"relevanceId\":\"urn:relevance:core\"}]")
    public Set<TopicFilterIndexDocument> filters = new HashSet<>();

    @JsonIgnore
    public URI topicFilterId, filterPublicId;

    @JsonIgnore
    public List<SubTopicIndexDocument> children = new ArrayList<>();

    private String language;

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

    public SubTopicIndexDocument(Subject subject, SubjectTopic subjectTopic, String language) {
        this.language = language;

        this.populateFromTopic(subjectTopic.getTopic());

        this.parent = subjectTopic.getSubject().getPublicId();
        this.connectionId = subjectTopic.getPublicId();
        this.isPrimary = subjectTopic.isPrimary();
        this.rank = subjectTopic.getRank();

        this.path = subjectTopic.getTopic().getPathByContext(subject).orElse(null);
    }


    public SubTopicIndexDocument(Subject subject, TopicSubtopic topicSubtopic, String language) {
        this.language = language;

        this.populateFromTopic(topicSubtopic.getSubtopic());

        this.parent = topicSubtopic.getTopic().getPublicId();
        this.connectionId = topicSubtopic.getPublicId();
        this.isPrimary = topicSubtopic.isPrimary();
        this.rank = topicSubtopic.getRank();

        this.path = topicSubtopic.getSubtopic().getPathByContext(subject).orElse(null);
    }

    private void populateFromTopic(Topic topic) {
        this.id = topic.getPublicId();
        this.name = topic.getTranslation(this.language)
                .map(TopicTranslation::getName)
                .orElse(topic.getName());
        this.contentUri = topic.getContentUri();

        topic.getTopicFilters().stream()
                .filter(topicFilter -> topicFilter.getFilter().isPresent())
                .filter(topicFilter -> topicFilter.getRelevance().isPresent())
                .forEach(topicFilter -> filters.add(new TopicFilterIndexDocument(topicFilter, this.language)));
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
