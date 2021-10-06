/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 - 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.service.MetadataIdField;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

public abstract class EntityWithPathDTO {
    @ApiModelProperty(value = "Node id", example = "urn:topic:234")
    @MetadataIdField
    private URI id;

    @ApiModelProperty(value = "The name of the node", example = "Trigonometry")
    private String name;

    @ApiModelProperty(value = "ID of content introducing this node. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    private URI contentUri;

    @ApiModelProperty(value = "The primary path for this node", example = "/subject:1/topic:1")
    private String path;

    @ApiModelProperty(value = "List of all paths to this node")
    private Set<String> paths;

    @ApiModelProperty(value = "Metadata for entity. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataDto metadata;

    @ApiModelProperty(value = "Relevance id", example = "urn:relevance:core")
    public URI relevanceId;

    public EntityWithPathDTO() {

    }

    public EntityWithPathDTO(EntityWithPath entity, String languageCode) {
        this.id = entity.getPublicId();
        this.contentUri = entity.getContentUri();
        this.paths = entity.getAllPaths();

        this.path = entity.getPrimaryPath()
                .orElse(null);

        this.name = entity.getTranslation(languageCode)
                .map(Translation::getName)
                .orElse(entity.getName());

        Optional<Relevance> relevance = entity.getParentConnection().flatMap(EntityWithPathConnection::getRelevance);
        this.relevanceId = relevance.map(Relevance::getPublicId).orElse(URI.create("urn:relevance:core"));
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

    public String getPath() {
        return path;
    }

    public Set<String> getPaths() {
        return paths;
    }

    public MetadataDto getMetadata() {
        return metadata;
    }

    public URI getRelevanceId() {
        return relevanceId;
    }

    protected void setMetadata(MetadataDto metadata) {
        this.metadata = metadata;
    }
}
