/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.rest.v1.NodeTranslations;
import no.ndla.taxonomy.rest.v1.NodeTranslations.TranslationDTO;
import no.ndla.taxonomy.service.TreeSorter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 *
 */
public abstract class EntityWithPathChildDTO implements TreeSorter.Sortable {
    public URI id;

    public String name;

    @Schema(description = "ID of article introducing this node. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @Schema(description = "Parent id in the current context, null if none exists")
    public URI parent;

    @Schema(description = "The primary path to this node.", example = "/subject:1/topic:1")
    public String path;

    @Schema(description = "List of all paths to this node")
    private TreeSet<String> paths;

    @Schema(description = "The id of the node connection which causes this node to be included in the result set.", example = "urn:node-connection:1")
    public URI connectionId;

    @Schema(description = "Primary connection", example = "true")
    public boolean isPrimary;

    @Schema(description = "The order in which to sort the node within it's level.", example = "1")
    public int rank;

    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public URI relevanceId;

    @Schema(description = "All translations of this resource")
    public TreeSet<NodeTranslations.TranslationDTO> translations;

    @Schema(description = "List of language codes supported by translations")
    public TreeSet<String> supportedLanguages;

    @JsonProperty
    @Schema(description = "The type of node", example = "resource")
    public NodeType nodeType;

    @JsonIgnore
    public List<EntityWithPathChildDTO> children = new ArrayList<>();

    private String language;

    @Schema(description = "Metadata for entity. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataDto metadata;

    public EntityWithPathChildDTO(Node node, NodeConnection nodeConnection, String language) {
        this.language = language;
        // This must be enabled when ed is updated to update metadata for connections.
        // this.metadata = new MetadataDto(nodeConnection.getMetadata());

        nodeConnection.getParent().ifPresent(parent -> this.parent = parent.getPublicId());

        nodeConnection.getChild().ifPresent(child -> {
            this.populateFromNode(child);
            this.path = child.getPathByContext(this.parent).orElse("");
            this.paths = child.getAllPaths();

            var translations = child.getTranslations();
            this.translations = translations.stream().map(TranslationDTO::new)
                    .collect(Collectors.toCollection(TreeSet::new));
            this.supportedLanguages = this.translations.stream().map(t -> t.language)
                    .collect(Collectors.toCollection(TreeSet::new));
            this.metadata = new MetadataDto(child.getMetadata());

            this.nodeType = child.getNodeType();
        });

        this.rank = nodeConnection.getRank();

        final Relevance relevance = nodeConnection.getRelevance().orElse(null);
        this.relevanceId = relevance != null ? relevance.getPublicId() : null;

        this.connectionId = nodeConnection.getPublicId();

        this.isPrimary = nodeConnection.isPrimary().orElse(false);
    }

    @Override
    @JsonIgnore
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof EntityWithPathChildDTO))
            return false;

        EntityWithPathChildDTO that = (EntityWithPathChildDTO) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public EntityWithPathChildDTO() {

    }

    public void setMetadata(MetadataDto metadata) {
        this.metadata = metadata;
    }

    public MetadataDto getMetadata() {
        return metadata;
    }

    private void populateFromNode(Node node) {
        this.id = node.getPublicId();
        this.name = node.getTranslation(this.language).map(Translation::getName).orElse(node.getName());
        this.contentUri = node.getContentUri();
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

    public URI getParent() {
        return parent;
    }

    public String getPath() {
        return path;
    }

    public Set<String> getPaths() {
        return paths;
    }

    public URI getConnectionId() {
        return connectionId;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public int getRank() {
        return rank;
    }

    public URI getRelevanceId() {
        return relevanceId;
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
