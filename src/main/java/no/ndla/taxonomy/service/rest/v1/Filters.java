package no.ndla.taxonomy.service.rest.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.repositories.FilterRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static no.ndla.taxonomy.service.jdbc.QueryUtils.*;
import static no.ndla.taxonomy.service.rest.v1.DocStrings.LANGUAGE_DOC;

@RestController
@RequestMapping(path = "/v1/filters")
@Transactional
public class Filters {

    FilterRepository filterRepository;
    private JdbcTemplate jdbcTemplate;
    private static final String GET_FILTERS_QUERY = getQuery("get_filters");

    public Filters(FilterRepository filterRepository, JdbcTemplate jdbcTemplate) {
        this.filterRepository = filterRepository;
        this.jdbcTemplate = jdbcTemplate;
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
        List<Object> args = asList(id.toString());

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
                        }});
                    }
                    return result;
                }
        );
    }


    public static class FilterIndexDocument {
        @JsonProperty
        @ApiModelProperty(example = "urn:filter:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The name of the filter", example = "1T-YF")
        public String name;

    }
}
