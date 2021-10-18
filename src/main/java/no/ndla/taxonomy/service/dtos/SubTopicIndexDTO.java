/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.TopicSubtopic;
import no.ndla.taxonomy.domain.TopicTranslation;
import no.ndla.taxonomy.service.MetadataIdField;
import no.ndla.taxonomy.service.TreeSorter;

import java.net.URI;
import java.util.Set;

/** */
@ApiModel("SubTopicIndexDocument")
public class SubTopicIndexDTO implements TreeSorter.Sortable {
    @JsonProperty
    @ApiModelProperty(value = "Topic id", example = "urn:topic:234")
    @MetadataIdField
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

    @JsonProperty
    @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
    public URI relevanceId;

    @ApiModelProperty(value = "Metadata for entity. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataDto metadata;

    @JsonProperty
    @ApiModelProperty(value = "List of all paths to this subtopic")
    private Set<String> paths;

    @JsonProperty
    @ApiModelProperty(value = "The primary path for this subtopic", example = "/subject:1/topic:1")
    private String path;

    private int rank;
    private URI parentId;

    public SubTopicIndexDTO() {
    }

    public SubTopicIndexDTO(TopicSubtopic topicSubtopic, String language) {
        topicSubtopic.getSubtopic().ifPresent(topic -> {
            this.id = topic.getPublicId();

            this.name = topic.getTranslation(language).map(TopicTranslation::getName).orElse(topic.getName());

            this.contentUri = topic.getContentUri();
            this.paths = topic.getAllPaths();
            this.path = topic.getPrimaryPath().orElse(null);
        });

        this.isPrimary = true;

        this.rank = topicSubtopic.getRank();

        topicSubtopic.getTopic().ifPresent(topic -> this.parentId = topic.getPublicId());

        {
            final Relevance relevance = topicSubtopic.getRelevance().orElse(null);
            this.relevanceId = relevance != null ? relevance.getPublicId() : null;
        }
    }

    public void setMetadata(MetadataDto metadata) {
        this.metadata = metadata;
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
        return parentId;
    }
}
