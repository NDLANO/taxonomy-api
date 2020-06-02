package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.DomainObject;
import no.ndla.taxonomy.repositories.TaxonomyRepository;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import no.ndla.taxonomy.service.MetadataApiService;
import no.ndla.taxonomy.service.MetadataUpdateService;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.dtos.RecursiveMergeResultDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

public class PathResolvableEntityRestController<T extends DomainObject> extends CrudController<T> {
    private final MetadataApiService metadataApiService;
    private final MetadataUpdateService metadataUpdateService;

    PathResolvableEntityRestController(TaxonomyRepository<T> taxonomyRepository, MetadataUpdateService metadataUpdateService, CachedUrlUpdaterService cachedUrlUpdaterService) {
        super(taxonomyRepository, cachedUrlUpdaterService);

        this.metadataApiService = metadataUpdateService.getMetadataApiService();
        this.metadataUpdateService = metadataUpdateService;
    }

    @GetMapping("/{id}/metadata")
    public MetadataDto getMetadata(@PathVariable("id") URI id) {
        return metadataApiService.getMetadataByPublicId(id);
    }

    @PutMapping("/{id}/metadata")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public MetadataDto putMetadata(@PathVariable("id") URI id, @RequestBody MetadataDto entityToUpdate) {
        return metadataApiService.updateMetadataByPublicId(id, entityToUpdate);
    }

    @PutMapping("/{id}/metadata-recursive")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public RecursiveMergeResultDto updateRecursively(@PathVariable("id") URI id,
                                                     @ApiParam(value = "Apply also to resources (even if having multiple topics as parent)", defaultValue = "false")
                                                     @RequestParam(value = "applyToResources", required = false, defaultValue = "false") boolean applyToResources,
                                                     @RequestBody MetadataDto metadataToMerge) {
        return metadataUpdateService.updateMetadataRecursivelyByPublicId(id, metadataToMerge, applyToResources);
    }
}
