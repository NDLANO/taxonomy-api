package no.ndla.taxonomy.service.rest.v1;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;

import static no.ndla.taxonomy.service.jdbc.QueryUtils.getQuery;
import static no.ndla.taxonomy.service.jdbc.QueryUtils.setQueryParameters;

@RestController
@RequestMapping("/v1/url/generate")
public class UrlGenerator {

    private static final String GENERATE_URL_QUERY = getQuery("generate_url");
    private JdbcTemplate jdbcTemplate;

    public UrlGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public UrlResult generate(
            @RequestParam("id") URI id,
            @ApiParam(value = "If the element has several possible paths, select the one most like this one", example = "/subject:1/topic:1")
            @RequestParam(required = false, defaultValue = "") String context
    ) throws MalformedURLException {

        String thePath = jdbcTemplate.query(GENERATE_URL_QUERY, setQueryParameters(Collections.singletonList(id.toString())),
                resultSet -> {
                    StringBuilder result = new StringBuilder();
                    while (resultSet.next()) {
                        result.append("/");
                        result.append(resultSet.getString("public_id").substring(4));
                    }
                    return result.toString();
                }
        );

        URI theId = id;
        return new UrlResult() {{
            id = theId;
            path = thePath;
        }};
    }

    public static class UrlResult {
        @ApiModelProperty(value = "ID of the element you requested a path to", example = "urn:resource:1")
        public URI id;

        @ApiModelProperty(value = "The calculated path to the element", example = "/subject:1/topic:1/resource:1")
        public String path;
    }
}
