package no.ndla.taxonomy.rest.v1.dtos.subjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
@ApiModel("SubTopicIndexDocument")
public class SubTopicIndexDocument {
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
}
