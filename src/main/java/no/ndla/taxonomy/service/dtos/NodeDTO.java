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
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.rest.v1.dtos.searchapi.LanguageFieldDTO;
import no.ndla.taxonomy.rest.v1.dtos.searchapi.SearchableTaxonomyResourceType;
import no.ndla.taxonomy.rest.v1.dtos.searchapi.TaxonomyContextDTO;
import no.ndla.taxonomy.rest.v1.dtos.searchapi.TaxonomyCrumbDTO;
import no.ndla.taxonomy.util.PrettyUrlUtil;

@Schema(name = "Node")
public class NodeDTO {
    @Schema(description = "Node id", example = "urn:topic:234")
    private URI id;

    @Schema(description = "The stored name of the node", example = "Trigonometry")
    private String baseName;

    @Schema(description = "The possibly translated name of the node", example = "Trigonometry")
    private String name;

    @Schema(
            description = "ID of content introducing this node. Must be a valid URI, but preferably not a URL.",
            example = "urn:article:1")
    private Optional<URI> contentUri = Optional.empty();

    @Schema(description = "The primary path for this node. Can be empty if no context", example = "/subject:1/topic:1")
    private Optional<String> path = Optional.empty();

    @Schema(description = "List of all paths to this node")
    private List<String> paths;

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
    private List<String> breadcrumbs = new ArrayList<>();

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

    @JsonProperty
    @Schema(description = "The context object selected when fetching node")
    private Optional<TaxonomyContextDTO> context = Optional.empty();

    @Schema(description = "The language code for which name is returned", example = "nb")
    private String language;

    @JsonProperty
    @Schema(description = "Quality evaluation of the article")
    private Optional<QualityEvaluationDTO> qualityEvaluation = Optional.empty();

    @JsonProperty
    @Schema(description = "A number representing the average grade of all children nodes recursively.")
    private Optional<GradeAverageDTO> gradeAverage = Optional.empty();

    public NodeDTO() {}

    public NodeDTO(
            Optional<Node> root,
            Optional<Node> parent,
            Node entity,
            String languageCode,
            Optional<String> contextId,
            Optional<Boolean> includeContexts,
            boolean filterProgrammes,
            boolean isVisible,
            boolean newUrlSeparator) {

        var contexts = entity.getContexts();
        var visibleContexts =
                isVisible ? contexts.stream().filter(TaxonomyContext::isVisible).collect(Collectors.toSet()) : contexts;
        var filteredContexts = visibleContexts.stream()
                .filter(ctx -> !filterProgrammes || !ctx.rootId().contains(NodeType.PROGRAMME.getName()))
                .collect(Collectors.toSet());

        this.qualityEvaluation = QualityEvaluationDTO.fromNode(entity);
        this.gradeAverage = GradeAverageDTO.fromNode(entity);
        this.id = entity.getPublicId();
        this.contentUri = Optional.ofNullable(entity.getContentUri());

        this.paths = filteredContexts.stream().map(TaxonomyContext::path).collect(Collectors.toList());
        this.path = entity.getPrimaryPath().or(() -> paths.stream().findFirst());

        Optional<Relevance> relevance =
                entity.getParentConnections().stream().findFirst().flatMap(NodeConnection::getRelevance);
        var relevanceName = relevance.map(LanguageField::fromRelevance).orElse(new LanguageField<>());
        this.relevanceId = relevance.map(Relevance::getPublicId);

        this.translations = entity.getTranslations().stream()
                .map(TranslationDTO::new)
                .collect(Collectors.toCollection(TreeSet::new));
        this.supportedLanguages =
                this.translations.stream().map(t -> t.language).collect(Collectors.toCollection(TreeSet::new));

        this.language = languageCode; // TODO: Must handle programmes and subject better
        // this.language = supportedLanguages.contains(languageCode)
        //        ? languageCode
        //        : supportedLanguages.isEmpty() ? Constants.DefaultLanguage : supportedLanguages.first();

        this.baseName = entity.getName();

        this.name = entity.getTranslatedName(this.language);

        this.metadata = new MetadataDTO(entity.getMetadata());

        this.resourceTypes = entity.getResourceResourceTypes().stream()
                .sorted()
                .map(resourceType -> new ResourceTypeWithConnectionDTO(resourceType, this.language))
                .collect(Collectors.toCollection(TreeSet::new));

        this.nodeType = entity.getNodeType();

        Optional<TaxonomyContext> selected = entity.pickContext(contextId, parent, root, filteredContexts);
        selected.ifPresent(ctx -> {
            var contextDto = getTaxonomyContextDTO(entity, newUrlSeparator, ctx, relevanceName);

            // TODO: this changes the content in context breadcrumbs
            LanguageField<List<String>> breadcrumbList =
                    LanguageField.listFromLists(ctx.breadcrumbs(), LanguageField.fromNode(entity));
            this.breadcrumbs = breadcrumbList.containsKey(this.language)
                    ? breadcrumbList.get(this.language)
                    : breadcrumbList.get(Constants.DefaultLanguage);

            this.path = Optional.of(contextDto.path());
            this.relevanceId = Optional.of(contextDto.relevanceId());
            this.contextId = Optional.of(contextDto.contextId());
            this.url = Optional.of(contextDto.url());
            this.context = Optional.of(contextDto);
        });

        includeContexts.filter(Boolean::booleanValue).ifPresent(includeCtx -> {
            this.contexts = filteredContexts.stream()
                    .map(ctx -> getTaxonomyContextDTO(entity, newUrlSeparator, ctx, relevanceName))
                    .toList();
        });
    }

    private TaxonomyContextDTO getTaxonomyContextDTO(
            Node entity, boolean newUrlSeparator, TaxonomyContext ctx, LanguageField<String> finalRelevanceName) {
        var parentContexts = entity.getAllParentContexts();
        var parents = ctx.parentContextIds().stream()
                .map(parentCtxId -> {
                    var parent = parentContexts.stream()
                            .filter(c -> c.contextId().equals(parentCtxId))
                            .findFirst();
                    if (parent.isPresent()) {
                        var p = parent.get();
                        var url = PrettyUrlUtil.createPrettyUrl(
                                Optional.ofNullable(ctx.rootName()),
                                p.name(),
                                this.language,
                                parentCtxId,
                                p.nodeType(),
                                newUrlSeparator);
                        return new TaxonomyCrumbDTO(
                                URI.create(p.publicId()),
                                parentCtxId,
                                LanguageFieldDTO.fromLanguageField(p.name()),
                                p.path(),
                                url.orElse(p.path()));
                    } else {
                        return null;
                    }
                })
                .toList();
        return new TaxonomyContextDTO(
                entity.getPublicId(),
                entity.getPublicId(),
                URI.create(ctx.rootId()),
                LanguageFieldDTO.fromLanguageField(ctx.rootName()),
                ctx.path(),
                LanguageFieldDTO.fromLanguageFieldList(ctx.breadcrumbs()),
                entity.getContextType(),
                URI.create(ctx.relevanceId()),
                LanguageFieldDTO.fromLanguageField(finalRelevanceName),
                entity.getResourceTypes().stream()
                        .sorted((o1, o2) -> {
                            if (o1.getParent().isEmpty()) return -1;
                            if (o2.getParent().isEmpty()) return 1;
                            return 0;
                        })
                        .map(SearchableTaxonomyResourceType::new)
                        .toList(),
                ctx.parentIds().stream().map(URI::create).toList(),
                ctx.parentContextIds().stream().toList(),
                ctx.isPrimary(),
                ctx.isActive(),
                ctx.isVisible(),
                ctx.contextId(),
                ctx.rank(),
                ctx.connectionId(),
                PrettyUrlUtil.createPrettyUrl(
                                Optional.of(ctx.rootName()),
                                LanguageField.fromNode(entity),
                                this.language,
                                ctx.contextId(),
                                entity.getNodeType(),
                                newUrlSeparator)
                        .orElse(ctx.path()),
                parents);
    }

    public URI getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBaseName() {
        return baseName;
    }

    public Optional<URI> getContentUri() {
        return contentUri;
    }

    public Optional<String> getPath() {
        return path;
    }

    public List<String> getPaths() {
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

    public Optional<TaxonomyContextDTO> getContext() {
        return context;
    }

    public List<TaxonomyContextDTO> getContexts() {
        return contexts;
    }

    public Optional<String> getUrl() {
        return url;
    }

    public String getLanguage() {
        return language;
    }
}
