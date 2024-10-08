/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.ndla.taxonomy.config.Constants;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.ResourceResourceTypeRepository;
import no.ndla.taxonomy.rest.v1.commands.ResourcePostPut;
import no.ndla.taxonomy.service.*;
import no.ndla.taxonomy.service.dtos.NodeDTO;
import no.ndla.taxonomy.service.dtos.NodeWithParents;
import no.ndla.taxonomy.service.dtos.ResourceTypeWithConnectionDTO;
import no.ndla.taxonomy.service.dtos.SearchResultDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/v1/resources", "/v1/resources/"})
public class Resources extends CrudControllerWithMetadata<Node> {
    private final ResourceResourceTypeRepository resourceResourceTypeRepository;
    private final NodeService nodeService;
    private final NodeRepository nodeRepository;
    private final SearchService searchService;

    @Value(value = "${new.url.separator:false}")
    private boolean newUrlSeparator;

    public Resources(
            NodeRepository nodeRepository,
            ResourceResourceTypeRepository resourceResourceTypeRepository,
            ContextUpdaterService contextUpdaterService,
            NodeService nodeService,
            QualityEvaluationService qualityEvaluationService,
            SearchService searchService) {
        super(nodeRepository, contextUpdaterService, nodeService, qualityEvaluationService);

        this.resourceResourceTypeRepository = resourceResourceTypeRepository;
        this.repository = nodeRepository;
        this.nodeRepository = nodeRepository;
        this.nodeService = nodeService;
        this.searchService = searchService;
    }

    @Override
    protected String getLocation() {
        return "/v1/resources";
    }

    @Deprecated
    @GetMapping
    @Operation(summary = "Lists all resources")
    @Transactional(readOnly = true)
    public List<NodeDTO> getAllResources(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    Optional<String> language,
            @Parameter(description = "Filter by contentUri") @RequestParam(value = "contentURI", required = false)
                    Optional<URI> contentUri,
            @Parameter(description = "Filter by key and value") @RequestParam(value = "key", required = false)
                    Optional<String> key,
            @Parameter(description = "Filter by key and value") @RequestParam(value = "value", required = false)
                    Optional<String> value,
            @Parameter(description = "Filter by visible") @RequestParam(value = "isVisible", required = false)
                    Optional<Boolean> isVisible) {
        MetadataFilters metadataFilters = new MetadataFilters(key, value, isVisible);
        return nodeService.getNodesByType(
                Optional.of(List.of(NodeType.RESOURCE)),
                language.orElse(Constants.DefaultLanguage),
                Optional.empty(),
                contentUri,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                metadataFilters,
                Optional.of(false),
                false,
                Optional.empty(),
                Optional.empty());
    }

    @Deprecated
    @GetMapping("/search")
    @Operation(summary = "Search all resources")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeDTO> searchResources(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    Optional<String> language,
            @Parameter(description = "How many results to return per page")
                    @RequestParam(value = "pageSize", defaultValue = "10")
                    int pageSize,
            @Parameter(description = "Which page to fetch") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Query to search names") @RequestParam(value = "query", required = false)
                    Optional<String> query,
            @Parameter(description = "Ids to fetch for query") @RequestParam(value = "ids", required = false)
                    Optional<List<String>> ids,
            @Parameter(description = "ContentURIs to fetch for query")
                    @RequestParam(value = "contentUris", required = false)
                    Optional<List<String>> contentUris) {
        return searchService.searchByNodeType(
                query,
                ids,
                contentUris,
                language,
                Optional.of(false),
                false,
                pageSize,
                page,
                Optional.of(List.of(NodeType.RESOURCE)),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    @Deprecated
    @GetMapping("/page")
    @Operation(summary = "Gets all resources paginated")
    @Transactional(readOnly = true)
    public SearchResultDTO<NodeDTO> getResourcePage(
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", defaultValue = Constants.DefaultLanguage, required = false)
                    Optional<String> language,
            @Parameter(name = "page", description = "The page to fetch") Optional<Integer> page,
            @Parameter(name = "pageSize", description = "Size of page to fetch") Optional<Integer> pageSize) {
        if (page.isEmpty() || pageSize.isEmpty()) {
            throw new IllegalArgumentException("Need both page and pageSize to return data");
        }
        if (page.get() < 1) throw new IllegalArgumentException("page parameter must be bigger than 0");

        var pageRequest = PageRequest.of(page.get() - 1, pageSize.get());
        var ids = nodeRepository.findIdsByTypePaginated(pageRequest, NodeType.RESOURCE);
        var results = nodeRepository.findByIds(ids.getContent());
        var contents = results.stream()
                .map(node -> new NodeDTO(
                        Optional.empty(),
                        Optional.empty(),
                        node,
                        language.orElse("nb"),
                        Optional.empty(),
                        Optional.of(false),
                        false,
                        false,
                        newUrlSeparator))
                .collect(Collectors.toList());
        return new SearchResultDTO<>(ids.getTotalElements(), page.get(), pageSize.get(), contents);
    }

    @Deprecated
    @GetMapping("{id}")
    @Operation(summary = "Gets a single resource")
    @Transactional(readOnly = true)
    public NodeDTO getResource(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    Optional<String> language) {
        return nodeService.getNode(id, language, Optional.empty(), Optional.empty(), Optional.of(false), false, true);
    }

    @Deprecated
    @PutMapping("{id}")
    @Operation(
            summary = "Updates a resource",
            security = {@SecurityRequirement(name = "oauth")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void updateResource(
            @PathVariable("id") URI id,
            @Parameter(
                            name = "resource",
                            description = "the updated resource. Fields not included will be set to null.")
                    @RequestBody
                    @Schema(name = "ResourcePOST")
                    ResourcePostPut command) {
        updateEntity(id, command);
    }

    @Deprecated
    @PostMapping
    @Operation(
            summary = "Adds a new resource",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> createResource(
            @Parameter(name = "resource", description = "the new resource") @RequestBody @Schema(name = "ResourcePUT")
                    ResourcePostPut command) {
        return createEntity(new Node(NodeType.RESOURCE), command);
    }

    @Deprecated
    @PostMapping("{id}/clone")
    @Operation(
            summary = "Clones a resource, including resource-types and translations",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> cloneResource(
            @Parameter(name = "id", description = "Id of resource to clone", example = "urn:resource:1")
                    @PathVariable("id")
                    URI publicId,
            @Parameter(name = "resource", description = "Object containing contentUri. Other values are ignored.")
                    @RequestBody
                    @Schema(name = "ResourcePOST")
                    ResourcePostPut command) {
        var entity = nodeService.cloneNode(publicId, Optional.ofNullable(command.contentUri));
        URI location = URI.create(getLocation() + "/" + entity.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @Deprecated
    @GetMapping("{id}/resource-types")
    @Operation(summary = "Gets all resource types associated with this resource")
    @Transactional(readOnly = true)
    public List<ResourceTypeWithConnectionDTO> getResourceTypes(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    Optional<String> language) {

        return resourceResourceTypeRepository.resourceResourceTypeByParentId(id).stream()
                .map(resourceResourceType -> new ResourceTypeWithConnectionDTO(
                        resourceResourceType, language.orElse(Constants.DefaultLanguage)))
                .toList();
    }

    @Deprecated
    @GetMapping("{id}/full")
    @Operation(summary = "Gets all parent topics, all filters and resourceTypes for this resource")
    @Transactional(readOnly = true)
    public NodeWithParents getResourceFull(
            @PathVariable("id") URI id,
            @Parameter(description = "ISO-639-1 language code", example = "nb")
                    @RequestParam(value = "language", required = false, defaultValue = Constants.DefaultLanguage)
                    Optional<String> language) {
        var node = nodeService.getNode(id);
        return new NodeWithParents(
                node, language.orElse(Constants.DefaultLanguage), Optional.of(false), newUrlSeparator);
    }

    @Deprecated
    @DeleteMapping("{id}")
    @Operation(
            summary = "Deletes a single entity by id",
            security = {@SecurityRequirement(name = "oauth")})
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteEntity(@PathVariable("id") URI id) {
        nodeService.delete(id);
    }
}
