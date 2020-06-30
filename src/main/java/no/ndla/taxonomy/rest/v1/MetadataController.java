package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.MetadataApiService;
import no.ndla.taxonomy.service.MetadataUpdateService;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import no.ndla.taxonomy.service.dtos.RecursiveMergeResultDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@Api(tags = {"topics", "resources", "subjects"})
public class MetadataController {
    private final MetadataApiService metadataApiService;
    private final MetadataUpdateService metadataUpdateService;

    public MetadataController(MetadataApiService metadataApiService, MetadataUpdateService metadataUpdateService) {
        this.metadataApiService = metadataApiService;
        this.metadataUpdateService = metadataUpdateService;
    }

    @GetMapping(
            path = {
                    "/v1/subjects/{id}/metadata",
                    "/v1/topics/{id}/metadata",
                    "/v1/resources/{id}/metadata",
            }
    )
    @ApiOperation(value = "Gets metadata for entity")
    public MetadataDto getMetadata(@PathVariable("id") URI id) {
        return metadataApiService.getMetadataByPublicId(id);
    }

    @PutMapping(
            path = {
                    "/v1/subjects/{id}/metadata",
                    "/v1/topics/{id}/metadata",
                    "/v1/resources/{id}/metadata",
            }
    )
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ApiOperation(tags = {"topics", "resources", "subjects"}, value = "Updates metadata for entity")
    public MetadataDto putMetadata(@PathVariable("id") URI id, @RequestBody MetadataDto entityToUpdate) {
        return metadataApiService.updateMetadataByPublicId(id, entityToUpdate);
    }

    @PutMapping(
            path = {
                    "/v1/subjects/{id}/metadata-recursive",
                    "/v1/topics/{id}/metadata-recursive",
                    "/v1/resources/{id}/metadata-recursive",
            }
    )
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ApiOperation(value = "Updates metadata for entity recursively")
    public RecursiveMergeResultDto updateRecursively(@PathVariable("id") URI id,
                                                     @ApiParam(value = "Apply also to resources (even if having multiple topics as parent)", defaultValue = "false")
                                                     @RequestParam(value = "applyToResources", required = false, defaultValue = "false") boolean applyToResources,
                                                     @RequestBody MetadataDto metadataToMerge) {
        return metadataUpdateService.updateMetadataRecursivelyByPublicId(id, metadataToMerge, applyToResources);
    }
}
