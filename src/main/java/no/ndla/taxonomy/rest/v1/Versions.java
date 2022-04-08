/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.domain.VersionType;
import no.ndla.taxonomy.repositories.VersionRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.rest.v1.commands.VersionCommand;
import no.ndla.taxonomy.service.VersionService;
import no.ndla.taxonomy.service.dtos.VersionDTO;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = { "/v1/versions" })
public class Versions extends CrudController<Version> {
    private final VersionRepository versionRepository;
    private final VersionService versionService;

    public Versions(VersionService versionService, VersionRepository versionRepository) {
        super(versionRepository);
        this.versionService = versionService;
        this.versionRepository = versionRepository;
    }

    @GetMapping
    @ApiOperation("Gets all versions")
    public List<VersionDTO> getAll(
            @ApiParam(value = "Version type", example = "PUBLISHED") @RequestParam(value = "type", required = false, defaultValue = "") Optional<VersionType> versionType,
            @ApiParam(value = "Version hash", example = "ndla") @RequestParam(value = "hash", required = false) Optional<String> hash) {
        if (versionType.isPresent())
            return versionService.getVersionsOfType(versionType.get());
        return hash.map(s -> List.of(versionRepository.findFirstByHash(s).map(VersionDTO::new).orElseThrow(() -> new NotFoundHttpResponseException("Version not found")))).orElseGet(versionService::getVersions);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single version")
    public VersionDTO get(@PathVariable("id") URI id) {
        return versionRepository.findFirstByPublicId(id).map(VersionDTO::new)
                .orElseThrow(() -> new NotFoundHttpResponseException("Version not found"));
    }

    @PostMapping
    @ApiOperation(value = "Creates a new version")
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    public ResponseEntity<Void> post(
            @ApiParam(value = "Base new version on version with this id") @RequestParam(value = "sourceId") Optional<URI> sourceId,
            @ApiParam(name = "version", value = "The new version") @RequestBody VersionCommand command) {
        // Don't call doPost because we need to create new schema
        Version version = versionService.createNewVersion(sourceId, command);
        URI location = URI.create(getLocation() + "/" + version.getPublicId());
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{id}")
    @ApiOperation("Updates a version")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    public void put(@PathVariable("id") URI id,
            @ApiParam(name = "version", value = "The updated version.") @RequestBody VersionCommand command) {
        doPut(id, command);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Deletes a single entity by id")
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void delete(@PathVariable("id") URI id) {
        Version version = versionRepository.getByPublicId(id);
        if (version == null || version.isLocked()) {
            throw new InvalidArgumentServiceException("Cannot delete locked version");
        }
        versionService.delete(id);
    }

    @PutMapping("/{id}/publish")
    @ApiOperation("Publishes a version")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_ADMIN')")
    public void publish(@PathVariable("id") URI id) {
        Version version = versionRepository.getByPublicId(id);
        if (version == null || version.getVersionType() != VersionType.BETA) {
            throw new InvalidArgumentServiceException("Version has wrong type");
        }
        versionService.publishBetaAndArchiveCurrent(id);
    }
}
