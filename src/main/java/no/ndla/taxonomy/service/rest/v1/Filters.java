package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.DuplicateIdException;
import no.ndla.taxonomy.service.domain.Filter;
import no.ndla.taxonomy.service.domain.Subject;
import no.ndla.taxonomy.service.domain.SubjectRequiredException;
import no.ndla.taxonomy.service.repositories.FilterRepository;
import no.ndla.taxonomy.service.repositories.SubjectRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.ndla.taxonomy.service.jdbc.QueryUtils.*;
import static no.ndla.taxonomy.service.rest.v1.DocStrings.LANGUAGE_DOC;

@RestController
@RequestMapping(path = {"/v1/filters"})
@Transactional
public class Filters {

    private FilterRepository filterRepository;
    private SubjectRepository subjectRepository;
    private JdbcTemplate jdbcTemplate;
    private static final String GET_FILTERS_QUERY = getQuery("get_filters");

    public Filters(FilterRepository filterRepository, JdbcTemplate jdbcTemplate, SubjectRepository subjectRepository) {
        this.filterRepository = filterRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.subjectRepository = subjectRepository;
    }

    @GetMapping
    @ApiOperation("Gets all filters")
    public List<FilterIndexDocument> index(
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        List<Object> args = singletonList(language);
        return getFilterIndexDocuments(GET_FILTERS_QUERY, args);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single filter", notes = "Default language will be returned if desired language not found or if parameter is omitted.")
    public FilterIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        String sql = GET_FILTERS_QUERY.replace("1 = 1", "f.public_id = ?");
        List<Object> args = asList(language, id.toString());

        return getFirst(getFilterIndexDocuments(sql, args), "Filter", id);
    }

    private List<FilterIndexDocument> getFilterIndexDocuments(String sql, List<Object> args) {
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

    @DeleteMapping("/{id}")
    @ApiOperation("Deletes a single filter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") URI id) throws Exception {
        Filter filter = filterRepository.getByPublicId(id);
        filterRepository.delete(filter);
    }

    @PostMapping
    @ApiOperation(value = "Creates a new filter")
    public ResponseEntity<Void> post(@ApiParam(name = "filter", value = "The new filter") @RequestBody CreateFilterCommand command) throws Exception {
        try {
            Filter filter = new Filter();
            if (null != command.id) filter.setPublicId(command.id);
            filter.setName(command.name);

            if (command.subjectId == null) {
                throw new SubjectRequiredException();
            }

            Subject subject = subjectRepository.getByPublicId(command.subjectId);
            filter.setSubject(subject);

            URI location = URI.create("/filters/" + filter.getPublicId());
            filterRepository.save(filter);
            return ResponseEntity.created(location).build();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateIdException("" + command.id);
        }
    }

    @PutMapping("/{id}")
    @ApiOperation("Updates a filter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "filter", value = "The updated filter") @RequestBody UpdateFilterCommand command
    ) throws Exception {
        Filter filter = filterRepository.getByPublicId(id);
        filter.setName(command.name);
        Subject subject = null;
        if (command.subjectId != null) {
            subject = subjectRepository.getByPublicId(command.subjectId);
        }
        filter.setSubject(subject);
    }


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

    public static class CreateFilterCommand {
        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:filter: and be a valid URI. If ommitted, an id will be assigned automatically.", example = "urn:filter:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the filter", example = "1T-YF")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "This filter will be connected to this subject.")
        public URI subjectId;
    }

    public static class UpdateFilterCommand {
        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the filter", example = "1T-YF")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "This filter will be connected to this subject.")
        public URI subjectId;
    }
}
