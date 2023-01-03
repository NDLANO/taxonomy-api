/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.NodeTranslation;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.service.TreeSorter;

import java.net.URI;
import java.util.Set;

/**
 *
 */
@Schema(name = "NodeConnectionIndexDocument")
public class NodeConnectionIndexDTO implements TreeSorter.Sortable {
    @JsonProperty
    @Schema(description = "Node id", example = "urn:topic:234")
    private URI id;

    @JsonProperty
    @Schema(description = "The name of the subnode", example = "Trigonometry")
    private String name;

    @JsonProperty
    @Schema(description = "ID of content introducing this node. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    private URI contentUri;

    @JsonProperty
    @Schema(description = "True if owned by this node, false if it has its primary connection elsewhere", example = "true")
    private Boolean isPrimary;

    @JsonProperty
    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public URI relevanceId;

    @Schema(description = "Metadata for entity. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataDto metadata;

    @JsonProperty
    @Schema(description = "List of all paths to this subnode")
    private Set<String> paths;

    @JsonProperty
    @Schema(description = "The primary path for this subnode", example = "/subject:1/topic:1")
    private String path;

    private int rank;
    private URI parentId;

    public NodeConnectionIndexDTO() {

    }

    public NodeConnectionIndexDTO(NodeConnection nodeConnection, String language) {
        nodeConnection.getChild().ifPresent(topic -> {
            this.id = topic.getPublicId();

            this.name = topic.getTranslation(language).map(NodeTranslation::getName).orElse(topic.getName());

            this.contentUri = topic.getContentUri();
            this.paths = topic.getAllPaths();
            this.path = topic.getPrimaryPath().orElse("");
        });

        this.isPrimary = true;

        this.rank = nodeConnection.getRank();
        this.metadata = new MetadataDto(nodeConnection.getMetadata());

        nodeConnection.getParent().ifPresent(topic -> this.parentId = topic.getPublicId());

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
