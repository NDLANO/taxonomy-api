package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.Relevance;
import no.ndla.taxonomy.service.repositories.RelevanceRepository;
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
@RequestMapping(path = {"/v1/relevances"})
@Transactional
public class Relevances extends CrudController<Relevance> {

    private JdbcTemplate jdbcTemplate;
    private static final String GET_RELEVANCES_QUERY = getQuery("get_relevances");

    public Relevances(RelevanceRepository repository, JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.repository = repository;
    }

    @GetMapping
    @ApiOperation("Gets all relevances")
    public List<RelevanceIndexDocument> index(
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        List<Object> args = singletonList(language);
        return getRelevanceIndexDocuments(GET_RELEVANCES_QUERY, args);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Gets a single relevance", notes = "Default language will be returned if desired language not found or if parameter is omitted.")
    public RelevanceIndexDocument get(
            @PathVariable("id") URI id,
            @ApiParam(value = LANGUAGE_DOC, example = "nb")
            @RequestParam(value = "language", required = false, defaultValue = "")
                    String language
    ) throws Exception {
        String sql = GET_RELEVANCES_QUERY.replace("1 = 1", "r.public_id = ?");
        List<Object> args = asList(language, id.toString());

        return getFirst(getRelevanceIndexDocuments(sql, args), "Relevance", id);
    }

    private List<RelevanceIndexDocument> getRelevanceIndexDocuments(String sql, List<Object> args) {
        return jdbcTemplate.query(sql, setQueryParameters(args),
                resultSet -> {
                    List<RelevanceIndexDocument> result = new ArrayList<>();
                    while (resultSet.next()) {
                        result.add(new RelevanceIndexDocument() {{
                            name = resultSet.getString("relevance_name");
                            id = getURI(resultSet, "relevance_public_id");
                        }});
                    }
                    return result;
                }
        );
    }

    @PostMapping
    @ApiOperation(value = "Creates a new relevance")
    public ResponseEntity<Void> post(@ApiParam(name = "relevance", value = "The new relevance") @RequestBody CreateRelevanceCommand command) throws Exception {
        return doPost(new Relevance(), command);
    }

    @PutMapping("/{id}")
    @ApiOperation("Updates a relevance")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(
            @PathVariable("id") URI id,
            @ApiParam(name = "relevance", value = "The updated relevance") @RequestBody UpdateRelevanceCommand command
    ) throws Exception {
        doPut(id, command);
    }

    public static class RelevanceIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:relevance:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the relevance", example = "1T-YF")
        public String name;

    }

    public static class CreateRelevanceCommand extends CreateCommand<Relevance> {
        @JsonProperty
        @ApiModelProperty(notes = "If specified, set the id to this value. Must start with urn:relevance: and be a valid URI. If ommitted, an id will be assigned automatically.", example = "urn:relevance:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the relevance", example = "1T-YF")
        public String name;

        @Override
        public URI getId() {
            return id;
        }

        @Override
        public void apply(Relevance entity) {
            entity.setName(name);
        }
    }

    public static class UpdateRelevanceCommand extends UpdateCommand<Relevance> {
        @JsonProperty
        @ApiModelProperty(required = true, value = "The name of the relevance", example = "1T-YF")
        public String name;

        @Override
        public void apply(Relevance entity) {
            entity.setName(name);
        }
    }
}