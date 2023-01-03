/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.EntityWithPath;
import no.ndla.taxonomy.domain.EntityWithPathConnection;
import no.ndla.taxonomy.domain.Relevance;
import no.ndla.taxonomy.domain.Translation;
import no.ndla.taxonomy.rest.v1.NodeTranslations.TranslationDTO;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public abstract class EntityWithPathDTO {
    @Schema(description = "Node id", example = "urn:topic:234")
    private URI id;

    @Schema(description = "The name of the node", example = "Trigonometry")
    private String name;

    @Schema(description = "ID of content introducing this node. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    private URI contentUri;

    @Schema(description = "The primary path for this node", example = "/subject:1/topic:1")
    private String path;

    @Schema(description = "List of all paths to this node")
    private TreeSet<String> paths;

    @Schema(description = "Metadata for entity. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataDto metadata;

    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public URI relevanceId;

    @Schema(description = "All translations of this node")
    private TreeSet<TranslationDTO> translations = new TreeSet<>();

    @Schema(description = "List of language codes supported by translations")
    private TreeSet<String> supportedLanguages;

    @Schema(description = "List of names in the path")
    private List<String> breadcrumbs;

    public EntityWithPathDTO() {
    }

    public EntityWithPathDTO(EntityWithPath entity, String languageCode) {
        this.id = entity.getPublicId();
        this.contentUri = entity.getContentUri();
        this.paths = entity.getAllPaths();

        this.path = entity.getPrimaryPath().orElse(this.paths.stream().findFirst().orElse(""));

        var translations = entity.getTranslations();
        this.translations = translations.stream().map(TranslationDTO::new)
                .collect(Collectors.toCollection(TreeSet::new));
        this.supportedLanguages = this.translations.stream().map(t -> t.language)
                .collect(Collectors.toCollection(TreeSet::new));

        this.name = translations.stream().filter(t -> Objects.equals(t.getLanguageCode(), languageCode)).findFirst()
                .map(Translation::getName).orElse(entity.getName());

        Optional<Relevance> relevance = entity.getParentConnection().flatMap(EntityWithPathConnection::getRelevance);
        this.relevanceId = relevance.map(Relevance::getPublicId).orElse(URI.create("urn:relevance:core"));

        this.metadata = new MetadataDto(entity.getMetadata());

        this.breadcrumbs = buildCrumbs(entity, languageCode);
    }

    private List<String> buildCrumbs(EntityWithPath entity, String languageCode) {
        List<String> parentCrumbs = entity.getParentConnection().flatMap(parentConnection -> parentConnection
                .getConnectedParent().map(parent -> buildCrumbs(parent, languageCode))).orElse(List.of());

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
