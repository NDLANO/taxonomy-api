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
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.rest.v1.dtos.searchapi.LanguageFieldDTO;
import no.ndla.taxonomy.rest.v1.dtos.searchapi.SearchableTaxonomyResourceType;
import no.ndla.taxonomy.rest.v1.dtos.searchapi.TaxonomyContextDTO;
import no.ndla.taxonomy.util.TitleUtil;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Schema(name = "Node")
public class NodeDTO {
    @Schema(description = "Node id", example = "urn:topic:234")
    private URI id;

    @Schema(description = "The name of the node", example = "Trigonometry")
    private String name;

    @Schema(description = "ID of content introducing this node. Must be a valid URI, but preferably not a URL.", example = "urn:article:1")
    private Optional<URI> contentUri = Optional.empty();

    @Schema(description = "The primary path for this node", example = "/subject:1/topic:1")
    private String path;

    @Schema(description = "List of all paths to this node")
    private TreeSet<String> paths;

    @Schema(description = "Metadata for entity. Read only.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataDTO metadata;

    @Schema(description = "Relevance id", example = "urn:relevance:core")
    public Optional<URI> relevanceId;

    @Schema(description = "All translations of this node")
    private TreeSet<TranslationDTO> translations = new TreeSet<>();

    @Schema(description = "List of language codes supported by translations")
    private TreeSet<String> supportedLanguages;

    @Schema(description = "List of names in the path")
    private List<String> breadcrumbs;

    @JsonProperty
    @Schema(description = "Resource type(s)", example = "[{\"id\": \"urn:resourcetype:1\",\"name\":\"lecture\"}]")
    private TreeSet<ResourceTypeWithConnectionDTO> resourceTypes = new TreeSet<>();

    @JsonProperty
    @Schema(description = "The type of node", example = "resource")
    public NodeType nodeType;

    @JsonProperty
    @Schema(description = "An id unique for this context.")
    private Optional<String> contextId = Optional.empty();

    @JsonProperty
    @Schema(description = "A pretty url based on name and context. Empty if no context.")
    private Optional<String> url = Optional.empty();

    @JsonProperty
    @Schema(description = "A list of all contexts this node is part of")
    private List<TaxonomyContextDTO> contexts = new ArrayList<>();

    public NodeDTO() {
    }

    public NodeDTO(Optional<Node> root, Node entity, String languageCode, Optional<String> contextId,
            Optional<Boolean> includeContexts) {
        this.id = entity.getPublicId();
        this.contentUri = Optional.ofNullable(entity.getContentUri());

        this.paths = entity.getAllPaths();

        this.path = entity.getPrimaryPath().orElse(this.paths.stream().findFirst().orElse(""));

        Optional<Relevance> relevance = entity.getParentConnections().stream().findFirst()
                .flatMap(NodeConnection::getRelevance);
        this.relevanceId = relevance.map(Relevance::getPublicId);

        var translations = entity.getTranslations();
        this.translations = translations.stream().map(TranslationDTO::new)
                .collect(Collectors.toCollection(TreeSet::new));
        this.supportedLanguages = this.translations.stream().map(t -> t.language)
                .collect(Collectors.toCollection(TreeSet::new));

        this.name = translations.stream().filter(t -> Objects.equals(t.getLanguageCode(), languageCode)).findFirst()
                .map(Translation::getName).orElse(entity.getName());

        this.metadata = new MetadataDTO(entity.getMetadata());

        this.resourceTypes = entity.getResourceResourceTypes().stream()
                .map(resourceType -> new ResourceTypeWithConnectionDTO(resourceType, languageCode))
                .collect(Collectors.toCollection(TreeSet::new));

        this.nodeType = entity.getNodeType();

        Optional<Context> context = entity.pickContext(contextId, root);
        context.ifPresent(ctx -> {
            this.path = ctx.path();
            this.breadcrumbs = LanguageField.listFromLists(ctx.breadcrumbs(), LanguageField.fromNode(entity))
                    .get(languageCode);
            this.relevanceId = Optional.of(URI.create(ctx.relevanceId()));
            this.contextId = Optional.of(ctx.contextId());
            this.url = TitleUtil.createPrettyUrl(this.name, ctx.contextId());
        });

        includeContexts.filter(Boolean::booleanValue).ifPresent(includeCtx -> {
            var relevanceName = new LanguageField<String>();
            if (relevance.isPresent()) {
                relevanceName = LanguageField.fromNode(relevance.get());
            }
            LanguageField<String> finalRelevanceName = relevanceName;
            this.contexts = entity.getContexts().stream().map(ctx -> {
                return new TaxonomyContextDTO(entity.getPublicId(), URI.create(ctx.rootId()),
                        LanguageFieldDTO.fromLanguageField(ctx.rootName()), ctx.path(),
                        LanguageFieldDTO.fromLanguageFieldList(ctx.breadcrumbs()), entity.getContextType(),
                        URI.create(ctx.relevanceId()), LanguageFieldDTO.fromLanguageField(finalRelevanceName),
                        entity.getResourceTypes().stream().map(SearchableTaxonomyResourceType::new).toList(),
                        ctx.parentIds().stream().map(URI::create).toList(), ctx.isPrimary(), ctx.isActive(),
                        ctx.isVisible(), ctx.contextId());
            }).toList();
        });
    }

    public URI getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Optional<URI> getContentUri() {
        return contentUri;
    }

    public String getPath() {
        return path;
    }

    public Set<String> getPaths() {
        return paths;
    }

    public MetadataDTO getMetadata() {
        return metadata;
    }

    public Optional<URI> getRelevanceId() {
        return relevanceId;
    }

    public Set<TranslationDTO> getTranslations() {
        return translations;
    }

    public List<String> getBreadcrumbs() {
        return breadcrumbs;
    }

    public Set<ResourceTypeWithConnectionDTO> getResourceTypes() {
        return resourceTypes;
    }

    public Set<String> getSupportedLanguages() {
        return supportedLanguages;
    }

    public List<TaxonomyContextDTO> getContexts() {
        return contexts;
    }

    public Optional<String> getUrl() {
        return url;
    }
}
