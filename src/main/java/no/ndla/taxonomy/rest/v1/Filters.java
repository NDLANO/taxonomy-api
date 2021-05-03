package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.exceptions.SubjectRequiredException;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.service.exceptions.InvalidArgumentServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
public class Filters extends ObsoleteCrudController {
    private final SubjectRepository subjectRepository;

    public Filters(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @GetMapping("/v1/filters")
    @ApiOperation("Gets all filters")
    public List<Object> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return List.of();
    }

    @GetMapping("/v1/filters/{id}")
    @ApiOperation(value = "Gets a single filter", notes = "Default language will be returned if desired language not found or if parameter is omitted.")
    public Object get(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @PostMapping("/v1/filters")
    @ApiOperation(value = "Creates a new filter")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> post(@ApiParam(name = "filter", value = "The new filter") @RequestBody FilterDTO command) {
        throw new InvalidArgumentServiceException("Create filter's disabled");
    }

    @PutMapping("/v1/filters/{id}")
    @ApiOperation("Updates a filter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "filter", value = "The updated filter") @RequestBody FilterDTO command
    ) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @DeleteMapping("/v1/filters/{id}")
    @ApiOperation(value = "Delete a single filter by ID")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable("id") URI id) {
        throw new NotFoundHttpResponseException("Filter was not found");
    }

    @GetMapping("/v1/subjects/{subjectId}/filters")
    @ApiOperation(value = "Gets all filters for a subject", tags = {"subjects"})
    public List<Object> getFiltersBySubjectId(
            @PathVariable("subjectId") URI subjectId,

            @ApiParam(value = "ISO-639-1 language code", example = "nb")
                    String language) {
        return List.of();
    }

    @GetMapping("/v1/resources/{resourceId}/filters")
    @ApiOperation(value = "Gets all filters associated with this resource", tags = {"resources"})
    public List<Object> getFiltersByResourceId(
            @PathVariable("resourceId")
                    URI resourceId,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return List.of();
    }

    @Override
    protected String getLocation() {
        return "/v1/filters";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class FilterDTO {
    }
}
