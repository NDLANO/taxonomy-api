/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.rest.v1.NodeTranslations.TranslationDTO;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public abstract class EntityWithPathDTO {
    @ApiModelProperty(value = "Node id", example = "urn:topic:234")
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

    @ApiModelProperty(value = "All translations of this node")
    private Set<TranslationDTO> translations;

    @ApiModelProperty(value = "List of language codes supported by translations")
    private Set<String> supportedLanguages;

    @ApiModelProperty(value = "List of names in the path")
    private List<String> breadcrumbs;

    public EntityWithPathDTO() {
    }

    public EntityWithPathDTO(EntityWithPath entity, String languageCode) {
        this.id = entity.getPublicId();
        this.contentUri = entity.getContentUri();
        this.paths = entity.getAllPaths();

        this.path = entity.getPrimaryPath().orElse(this.paths.stream().findFirst().orElse(""));

        var translations = entity.getTranslations();
        this.translations = translations.stream().map(TranslationDTO::new).collect(Collectors.toSet());
        this.supportedLanguages = this.translations.stream().map(t -> t.language).collect(Collectors.toSet());

        this.name = translations.stream().filter(t -> Objects.equals(t.getLanguageCode(), languageCode)).findFirst()
                .map(Translation::getName).orElse(entity.getName());

        Optional<Relevance> relevance = entity.getParentConnection().flatMap(EntityWithPathConnection::getRelevance);
        this.relevanceId = relevance.map(Relevance::getPublicId).orElse(URI.create("urn:relevance:core"));

        this.metadata = new MetadataDto(entity.getMetadata());

        this.breadcrumbs = buildCrumbs(entity, languageCode);
    }

    private List<String> buildCrumbs(EntityWithPath entity, String languageCode) {
        List<String> parentCrumbs = entity.getParentConnection()
                .flatMap(parentConnection -> parentConnection.getConnectedParent().map(parent -> buildCrumbs(parent, languageCode)))
                .orElse(List.of());

        var crumbs = new ArrayList<>(parentCrumbs);
        var name = entity.getTranslation(languageCode).map(Translation::getName).orElse(entity.getName());
        crumbs.add(name);
        return crumbs;
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

    public Set<TranslationDTO> getTranslations() {
        return translations;
    }

    public List<String> getBreadcrumbs() {
        return breadcrumbs;
    }

    public Set<String> getSupportedLanguages() {
        return supportedLanguages;
    }

    protected void setMetadata(MetadataDto metadata) {
        this.metadata = metadata;
    }
}
