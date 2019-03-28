package no.ndla.taxonomy.rest.v1.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.Filter;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.SubjectRequiredException;
import no.ndla.taxonomy.repositories.FilterRepository;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.rest.v1.commands.CreateCommand;
import no.ndla.taxonomy.rest.v1.commands.UpdateCommand;
import no.ndla.taxonomy.services.PublicIdGeneratorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static no.ndla.taxonomy.jdbc.QueryUtils.*;

@RestController
@RequestMapping(path = {"/v1/filters"})
@Transactional
public class Filters extends CrudController<Filter> {

    private SubjectRepository subjectRepository;
    private JdbcTemplate jdbcTemplate;
    private static final String GET_FILTERS_QUERY = getQuery("get_filters");

    public Filters(FilterRepository repository, JdbcTemplate jdbcTemplate, SubjectRepository subjectRepository,
                   PublicIdGeneratorService publicIdGeneratorService) {
        this.jdbcTemplate = jdbcTemplate;
        this.subjectRepository = subjectRepository;
        this.repository = repository;
        this.publicIdGeneratorService = publicIdGeneratorService;
    }

    @GetMapping
    @ApiOperation("Gets all filters")
    public List<FilterIndexDocument> index(
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        return getFilterIndexDocuments(GET_FILTERS_QUERY, language);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single filter", notes = "Default language will be returned if desired language not found or if parameter is omitted.")
    public FilterIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = "ISO-639-1 language code", example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        String sql = GET_FILTERS_QUERY.replace("1 = 1", "f.public_id = ?");
        return getFirst(getFilterIndexDocuments(sql, language, id.toString()), "Filter", id);
    }

    private List<FilterIndexDocument> getFilterIndexDocuments(String sql, Object... args) {
        return jdbcTemplate.query(sql, setQueryParameters(args),
                resultSet -> {
                    List<FilterIndexDocument> result = new ArrayList<>();
                    while (resultSet.next()) {
                        result.add(new FilterIndexDocument() {{
                            name = resultSet.getString("filter_name");
                            id = getURI(resultSet, "filter_public_id");
                            subjectId = getURI(resultSet, "subject_id");
                        }});
                    }
                    return result;
                }
        );
    }

    @PostMapping
    @ApiOperation(value = "Creates a new filter")
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public ResponseEntity<Void> post(@ApiParam(name = "filter", value = "The new filter") @RequestBody CreateFilterCommand command) throws Exception {
        if (command.subjectId == null) throw new SubjectRequiredException();

        Filter filter = new Filter();
        if (command.id == null) {
            command.id = publicIdGeneratorService.getNext("urn:filter");
        }
        Subject subject = subjectRepository.getByPublicId(command.subjectId);
        filter.setSubject(subject);
        return doPost(filter, command);
    }

    @PutMapping("/{id}")
    @ApiOperation("Updates a filter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "filter", value = "The updated filter") @RequestBody UpdateFilterCommand command
    ) throws Exception {
        if (command.subjectId == null) throw new SubjectRequiredException();

        Filter filter = doPut(id, command);
        filter.setSubject(subjectRepository.getByPublicId(command.subjectId));
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

        @Override
        public URI getId() {
            return id;
        }

        @Override
        public void apply(Filter filter) {
            filter.setName(name);
        }
    }

    public static class UpdateFilterCommand extends UpdateCommand<Filter> {
        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the filter", example = "1T-YF")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "This filter will be connected to this subject. Fields not included will be set to null.")
        public URI subjectId;

        @Override
        public void apply(Filter filter) {
            filter.setName(name);
        }
    }
}
