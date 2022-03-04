/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.Translation;
import no.ndla.taxonomy.rest.v1.NodeTranslations;
import no.ndla.taxonomy.rest.v1.NodeTranslations.TranslationDTO;
import no.ndla.taxonomy.service.TreeSorter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
public abstract class EntityWithPathChildDTO implements TreeSorter.Sortable {
    public URI id;

    public String name;

    @ApiModelProperty(value = "ID of article introducing this node. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    public URI contentUri;

    @ApiModelProperty("Parent id in the current context, null if none exists")
    public URI parent;

    @ApiModelProperty(value = "The primary path to this node.", example = "/subject:1/topic:1")
    public String path;

    @ApiModelProperty(value = "List of all paths to this node")
    private Set<String> paths;

    @ApiModelProperty(value = "The id of the node connection which causes this node to be included in the result set.", example = "urn:node-connection:1")
    public URI connectionId;

    @ApiModelProperty(value = "Primary connection", example = "true")
    public boolean isPrimary;

    @ApiModelProperty(value = "The order in which to sort the node within it's level.", example = "1")
    public int rank;

    @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
    public URI relevanceId;

    @ApiModelProperty(value = "All translations of this resource")
    public Set<NodeTranslations.TranslationDTO> translations;

    @ApiModelProperty(value = "List of language codes supported by translations")
    public Set<String> supportedLanguages;

    @JsonIgnore
    public List<EntityWithPathChildDTO> children = new ArrayList<>();

    private String language;

    @ApiModelProperty(value = "Metadata for entity. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataDto metadata;

    public EntityWithPathChildDTO(Node node, NodeConnection nodeConnection, String language) {
        this.language = language;

        nodeConnection.getChild().ifPresent(child -> {
            this.populateFromNode(child);
            this.path = child.getPathByContext(node).orElse("");
            this.paths = child.getAllPaths();
            this.metadata = new MetadataDto(child.getMetadata());

            var translations = child.getTranslations();
            this.translations = translations.stream().map(TranslationDTO::new).collect(Collectors.toSet());
            this.supportedLanguages = this.translations.stream().map(t -> t.language).collect(Collectors.toSet());
        });

        nodeConnection.getParent().ifPresent(parent -> this.parent = parent.getPublicId());

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
