package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.DomainObject;
import no.ndla.taxonomy.service.MetadataApiService;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;

public class PathResolvableEntityRestController<T extends DomainObject> extends CrudController<T> {
    private final MetadataApiService metadataApiService;

    PathResolvableEntityRestController(MetadataApiService metadataApiService) {
        this.metadataApiService = metadataApiService;
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
}
