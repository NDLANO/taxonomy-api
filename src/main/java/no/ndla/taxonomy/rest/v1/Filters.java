package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.FilterTranslation;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.domain.exceptions.SubjectRequiredException;
import no.ndla.taxonomy.repositories.FilterRepository;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.rest.v1.commands.CreateCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateCommand;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = {"/v1/filters"})
@Transactional
public class Filters extends CrudController<Filter> {

    private final SubjectRepository subjectRepository;
    private final FilterRepository filterRepository;

    public Filters(FilterRepository repository, SubjectRepository subjectRepository) {
        super(repository);

        this.subjectRepository = subjectRepository;
        this.filterRepository = repository;
    }

    @GetMapping
    @ApiOperation("Gets all filters")
    public List<FilterIndexDocument> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return filterRepository.findAllWithSubjectAndTranslations()
                .stream()
                .map((filter) -> new FilterIndexDocument(filter, language))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single filter", notes = "Default language will be returned if desired language not found or if parameter is omitted.")
    public FilterIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) {
        return filterRepository.findFirstByPublicIdWithSubjectAndTranslations(id)
                .map((filter) -> new FilterIndexDocument(filter, language))
                .orElseThrow(() -> new NotFoundException("Filter", id));
    }

    @PostMapping
    @ApiOperation(value = "Creates a new filter")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(@ApiParam(name = "filter", value = "The new filter") @RequestBody CreateFilterCommand command) {
        if (command.subjectId == null) throw new SubjectRequiredException();

        Filter filter = new Filter();
        Subject subject = subjectRepository.getByPublicId(command.subjectId);
        subject.addFilter(filter);
        return doPost(filter, command);
    }

    @PutMapping("/{id}")
    @ApiOperation("Updates a filter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "filter", value = "The updated filter") @RequestBody UpdateFilterCommand command
    ) {
        if (command.subjectId == null) throw new SubjectRequiredException();

        Filter filter = doPut(id, command);
        filter.setSubject(subjectRepository.getByPublicId(command.subjectId));

        repository.save(filter);
    }

    @ApiModel("Filters.FilterIndexDocument")
    public static class FilterIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:filter:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the filter", example = "1T-YF")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "The id of the connected subject", example = "urn:subject:1")
        public URI subjectId;

        @JsonProperty
        @ApiModelProperty(value = "ID of frontpage introducing this filter.", example = "urn:frontpage:1")
        public URI contentUri;

        public FilterIndexDocument() {
        }

        public FilterIndexDocument(Filter filter, String translation) {
            name = filter.getTranslation(translation)
                    .map(FilterTranslation::getName)
                    .orElse(filter.getName());

            id = filter.getPublicId();
            subjectId = filter.getSubject()
                    .map(Subject::getPublicId)
                    .orElse(null);
            contentUri = filter.getContentUri();
        }

        public URI getId() {
            return id;
        }
    }

    public static class CreateFilterCommand extends CreateCommand<Filter> {
        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:filter: and be a valid URI. If ommitted, an id will be assigned automatically.", example = "urn:filter:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the filter", example = "1T-YF")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "This filter will be connected to this subject.")
        public URI subjectId;

        @JsonProperty
        @ApiModelProperty(value = "ID of frontpage introducing this filter.", example = "urn:frontpage:1")
        public URI contentUri;

        @Override
        public URI getId() {
            return id;
        }

        @Override
        public void apply(Filter filter) {
            filter.setName(name);
            filter.setContentUri(contentUri);
        }
    }

    public static class UpdateFilterCommand extends UpdateCommand<Filter> {
        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the filter", example = "1T-YF")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "This filter will be connected to this subject. Fields not included will be set to null.")
        public URI subjectId;

        @JsonProperty
        @ApiModelProperty(value = "ID of frontpage introducing this filter.", example = "urn:frontpage:1")
        public URI contentUri;

        @Override
        public void apply(Filter filter) {
            filter.setName(name);
            filter.setContentUri(contentUri);
        }
    }
}
