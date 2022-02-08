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
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeTranslation;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.service.TreeSorter;

import java.net.URI;
import java.util.Set;

/**
 *
 */
@ApiModel("ChildIndexDocument")
public class ChildIndexDTO implements TreeSorter.Sortable {
    @JsonProperty
    @ApiModelProperty(value = "Node id", example = "urn:topic:234")
    private URI id;

    @JsonProperty
    @ApiModelProperty(value = "The name of the child node", example = "Trigonometry")
    private String name;

    @JsonProperty
    @ApiModelProperty(value = "ID of content introducing this node. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    private URI contentUri;

    @JsonProperty
    @ApiModelProperty(value = "True if owned by this node, false if it has its primary connection elsewhere", example = "true")
    private Boolean isPrimary;

    @JsonProperty
    @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
    public URI relevanceId;

    @ApiModelProperty(value = "Metadata for entity. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataDto metadata;

    @JsonProperty
    @ApiModelProperty(value = "List of all paths to this child node")
    private Set<String> paths;

    @JsonProperty
    @ApiModelProperty(value = "The primary path for this child node", example = "/subject:1/topic:1")
    private String path;

    private int rank;

    private URI parentId;

    public ChildIndexDTO() {

    }

    public ChildIndexDTO(NodeConnection nodeConnection, String language) {
        nodeConnection.getChild().ifPresent(child -> {
            this.id = child.getPublicId();

            this.name = child.getTranslation(language).map(NodeTranslation::getName).orElse(child.getName());

            this.contentUri = child.getContentUri();
            this.paths = child.getAllPaths();
            this.path = child.getPrimaryPath().orElse(null);
        });

        this.isPrimary = true;

        this.rank = nodeConnection.getRank();

        nodeConnection.getParent().ifPresent(parent -> this.parentId = parent.getPublicId());

        {
            final Relevance relevance = nodeConnection.getRelevance().orElse(null);
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
