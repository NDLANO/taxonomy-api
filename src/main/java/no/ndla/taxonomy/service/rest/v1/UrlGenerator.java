package no.ndla.taxonomy.service.rest.v1;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static no.ndla.taxonomy.service.jdbc.QueryUtils.getQuery;
import static no.ndla.taxonomy.service.jdbc.QueryUtils.setQueryParameters;

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
    ) throws MalformedURLException {
        Collection<String> urls =
                jdbcTemplate.query(GENERATE_URL_QUERY, setQueryParameters(Collections.singletonList(id.toString())),
                        resultSet -> {
                            Map<String, String> result = new HashMap<>();
                            while (resultSet.next()) {
                                String publicId = resultSet.getString("public_id");
                                String childId = resultSet.getString("child_public_id");
                                String prefix = "";
                                if (result.containsKey(publicId)) {
                                    prefix = result.get(publicId);
                                    result.remove(publicId);
                                }
                                String path = prefix + "/" + publicId.substring(4);
                                result.put(childId, path);
                            }
                            return result.values();
                        }
                );

        return new UrlResult(id, getUrlMatchingContext(context, urls));
    }

    private String getUrlMatchingContext(String context, Collection<String> urls) {
        String longestPrefix = "";
        String bestUrl = "";
        for (String possibleUrl : urls) {
            String commonPrefix = StringUtils.getCommonPrefix(context, possibleUrl);
            if (commonPrefix.length() >= longestPrefix.length()) {
                bestUrl = possibleUrl;
                longestPrefix = commonPrefix;
            }
        }
        return bestUrl;
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
