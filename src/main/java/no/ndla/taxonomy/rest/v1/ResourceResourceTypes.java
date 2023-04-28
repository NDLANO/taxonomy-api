/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import no.ndla.taxonomy.domain.ResourceResourceType;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.repositories.ResourceResourceTypeRepository;
import no.ndla.taxonomy.repositories.ResourceTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = { "/v1/resource-resourcetypes" })
public class ResourceResourceTypes {

    private final ResourceResourceTypeRepository resourceResourceTypeRepository;
    private final ResourceTypeRepository resourceTypeRepository;
    private final NodeRepository nodeRepository;

    public ResourceResourceTypes(ResourceResourceTypeRepository resourceResourceTypeRepository,
            ResourceTypeRepository resourceTypeRepository, NodeRepository nodeRepository) {
        this.resourceResourceTypeRepository = resourceResourceTypeRepository;
        this.resourceTypeRepository = resourceTypeRepository;
        this.nodeRepository = nodeRepository;
    }

    @PostMapping
    @Operation(summary = "Adds a resource type to a resource", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> post(
            @Parameter(name = "connection", description = "The new resource/resource type connection") @RequestBody ResourceResourceTypePOST command) {

        var resource = nodeRepository.getByPublicId(command.resourceId);

        ResourceType resourceType = resourceTypeRepository.getByPublicId(command.resourceTypeId);

        ResourceResourceType resourceResourceType = resource.addResourceType(resourceType);
        resourceResourceTypeRepository.save(resourceResourceType);

        URI location = URI.create("/resource-resourcetypes/" + resourceResourceType.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping({ "/{id}" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Removes a resource type from a resource", security = { @SecurityRequirement(name = "oauth") })
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void delete(@PathVariable("id") URI id) {
        resourceResourceTypeRepository.delete(resourceResourceTypeRepository.getByPublicId(id));
        resourceResourceTypeRepository.flush();
    }

    @GetMapping
    @Operation(summary = "Gets all connections between resources and resource types")
    @Transactional(readOnly = true)
    public List<ResourceResourceTypeDTO> index() {
        return resourceResourceTypeRepository.findAllIncludingResourceAndResourceType().stream()
                .map(ResourceResourceTypeDTO::new).collect(Collectors.toList());
    }

    @GetMapping({ "/{id}" })
    @Operation(summary = "Gets a single connection between resource and resource type")
    @Transactional(readOnly = true)
    public ResourceResourceTypeDTO get(@PathVariable("id") URI id) {
        ResourceResourceType result = resourceResourceTypeRepository.getByPublicId(id);
        return new ResourceResourceTypeDTO(result);
    }

    public static class ResourceResourceTypePOST {
        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource id", example = "urn:resource:123")
        URI resourceId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource type id", example = "urn:resourcetype:234")
        URI resourceTypeId;
    }

    @Schema(name = "ResourceResourceType")
    public static class ResourceResourceTypeDTO {
        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource type id", example = "urn:resource:123")
        URI resourceId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource type id", example = "urn:resourcetype:234")
        URI resourceTypeId;

        @JsonProperty
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Resource to resource type connection id", example = "urn:resource-has-resourcetypes:12")
        URI id;

        public ResourceResourceTypeDTO() {
        }

        public ResourceResourceTypeDTO(ResourceResourceType resourceResourceType) {
            id = resourceResourceType.getPublicId();
            resourceId = resourceResourceType.getNode().getPublicId();
            resourceTypeId = resourceResourceType.getResourceType().getPublicId();
        }
    }
}
