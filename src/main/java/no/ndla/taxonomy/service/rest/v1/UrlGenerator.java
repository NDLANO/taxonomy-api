package no.ndla.taxonomy.service.rest.v1;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.service.domain.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static no.ndla.taxonomy.service.jdbc.QueryUtils.getQuery;
import static no.ndla.taxonomy.service.jdbc.QueryUtils.setQueryParameters;
import static org.apache.commons.lang3.StringUtils.isBlank;

@RestController
@RequestMapping("/v1/url/generate")
@Transactional
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
    ) {
        UrlResult urlResult = getUrlResult(id, context);
        if (isBlank(urlResult.path)) throw new NotFoundException("Element", id);
        return urlResult;
    }

    public UrlResult getUrlResult(URI id, String context) {
        boolean onlyPrimary = isBlank(context);
        String[] urls = generatePaths(id, onlyPrimary);
        return new UrlResult(id, UrlResolver.getPathMostCloselyMatchingContext(context, urls));
    }


    private String[] generatePaths(URI publicId, boolean onlyPrimary) {
        String query = GENERATE_URL_QUERY;
        if (onlyPrimary) {
            query = query.replace("1 = 1", "parent.is_primary = true");
        }
        return jdbcTemplate.query(query, setQueryParameters(Collections.singletonList(publicId.toString())),
                resultSet -> {
                    Map<String, List<String>> urls = new HashMap<>();
                    while (resultSet.next()) {
                        mapRow(resultSet, urls);
                    }
                    List<String> paths = urls.values().stream().flatMap(List::stream).collect(Collectors.toList());
                    return paths.toArray(new String[paths.size()]);
                }
        );
    }

    private void mapRow(ResultSet resultSet, Map<String, List<String>> urls) throws SQLException {
        String publicId = resultSet.getString("public_id");
        String childId = resultSet.getString("child_public_id");

        List<String> prefixes = get(urls, publicId);
        urls.remove(publicId);
        if (prefixes.size() == 0) prefixes.add("");

        List<String> paths = get(urls, childId);
        urls.put(childId, paths);

        for (String prefix : prefixes) {
            paths.add(prefix + "/" + publicId.substring(4));
        }
    }

    private List<String> get(Map<String, List<String>> urls, String publicId) {
        return urls.containsKey(publicId) ? urls.get(publicId) : new ArrayList<>();
    }

    public static class UrlResult {
        public UrlResult() {
        }

        public UrlResult(URI id, String path) {
            this.id = id;
            this.path = path;
        }

        @ApiModelProperty(value = "ID of the element you requested a path to", example = "urn:resource:1")
        public URI id;

        @ApiModelProperty(value = "The calculated path to the element", example = "/subject:1/topic:1/resource:1")
        public String path;
    }
}
