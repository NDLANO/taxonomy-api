package no.ndla.taxonomy.rest.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.exceptions.SubjectRequiredException;
import no.ndla.taxonomy.repositories.FilterRepository;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.service.FilterService;
import no.ndla.taxonomy.service.dtos.FilterDTO;
import no.ndla.taxonomy.service.dtos.FilterWithConnectionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
public class Filters extends CrudController<Filter> {
    private final SubjectRepository subjectRepository;
    private final FilterService filterService;

    public Filters(FilterRepository repository, SubjectRepository subjectRepository, FilterService filterService) {
        super(repository);

        this.subjectRepository = subjectRepository;
        this.filterService = filterService;
    }

    @GetMapping("/v1/filters")
    @ApiOperation("Gets all filters")
    public List<FilterDTO> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return filterService.getFilters(language);
    }

    @GetMapping("/v1/filters/{id}")
    @ApiOperation(value = "Gets a single filter", notes = "Default language will be returned if desired language not found or if parameter is omitted.")
    public FilterDTO get(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return filterService.getFilterByPublicId(id, language);
    }

    @PostMapping("/v1/filters")
    @ApiOperation(value = "Creates a new filter")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @Transactional
    public ResponseEntity<Void> post(@ApiParam(name = "filter", value = "The new filter") @RequestBody FilterDTO command) {
        if (command.subjectId == null) throw new SubjectRequiredException();

        Filter filter = new Filter();
        Subject subject = subjectRepository.getByPublicId(command.subjectId);
        subject.addFilter(filter);
        return doPost(filter, command);
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
        if (command.subjectId == null) throw new SubjectRequiredException();

        Filter filter = doPut(id, command);
        filter.setSubject(subjectRepository.getByPublicId(command.subjectId));

        repository.save(filter);
    }

    @DeleteMapping("/v1/filters/{id}")
    @ApiOperation(value = "Delete a single filter by ID")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable("id") URI id) {
        repository.deleteByPublicId(id);
    }

    @GetMapping("/v1/subjects/{subjectId}/filters")
    @ApiOperation(value = "Gets all filters for a subject", tags = {"subjects"})
    public List<FilterDTO> getFiltersBySubjectId(
            @PathVariable("subjectId") URI subjectId,

            @ApiParam(value = "ISO-639-1 language code", example = "nb")
                    String language) {
        return filterService.getFiltersBySubjectId(subjectId, language);
    }

    @GetMapping("/v1/resources/{resourceId}/filters")
    @ApiOperation(value = "Gets all filters associated with this resource", tags = {"resources"})
    public List<FilterWithConnectionDTO> getFiltersByResourceId(
            @PathVariable("resourceId")
                    URI resourceId,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return filterService.getFiltersWithConnectionByResourceId(resourceId, language);
    }

    @Override
    protected String getLocation() {
        return "/v1/filters";
    }
}
