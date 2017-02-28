package no.ndla.taxonomy.service.rest.v1;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.service.domain.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.net.URI;
import java.util.Collections;

import static no.ndla.taxonomy.service.jdbc.QueryUtils.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

@RestController
@RequestMapping("/v1/url/resolve")
@Transactional
public class UrlResolver {

    private static final String RESOLVE_URL_QUERY = getQuery("resolve_url");

    private UrlGenerator urlGenerator;
    private JdbcTemplate jdbcTemplate;

    public UrlResolver(UrlGenerator urlGenerator, JdbcTemplate jdbcTemplate) {
        this.urlGenerator = urlGenerator;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResolvedUrl resolve(@RequestParam String url, HttpServletResponse response) throws Exception {
        URI id = getId(url);

        UrlGenerator.UrlResult urlResult = urlGenerator.getUrlResult(id, url);
        if (isBlank(urlResult.path)) throw new NotFoundException(url);

        if (!urlResult.path.equals(url)) {
            response.sendRedirect(urlResult.path);
            return null;
        }

        ResolvedUrl resolvedUrl = new ResolvedUrl();
        resolvedUrl.parents = getParents(urlResult);

        jdbcTemplate.query(RESOLVE_URL_QUERY, setQueryParameters(Collections.singletonList(id.toString())),
                resultSet -> {
                    if (resultSet.next()) {
                        resolvedUrl.id = getURI(resultSet, "public_id");
                        resolvedUrl.contentUri = getURI(resultSet, "content_uri");
                        resolvedUrl.name = resultSet.getString("name");
                    }
                    return resolvedUrl;
                }
        );

        return resolvedUrl;
    }

    private URI getId(String url) {
        String[] parts = url.split("/");
        return URI.create("urn:" + parts[parts.length - 1]);
    }

    private URI[] getParents(UrlGenerator.UrlResult urlResult) {
        String[] pathElements = urlResult.path.split("/");
        if (pathElements.length < 2) return new URI[0];

        URI[] result = new URI[pathElements.length - 2];
        for (int i = 1; i < pathElements.length - 1; i++) {
            String pathElement = pathElements[i];
            URI parent = URI.create("urn:" + pathElement);
            result[result.length - i] = parent;
        }
        return result;
    }

    public static class ResolvedUrl {
        @ApiModelProperty(value = "ID of the element referred to by the given path", example = "urn:resource:1")
        public URI id;

        @JsonProperty
        @ApiModelProperty(value = "The ID of this element in the system where the content is stored. ",
                notes = "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                        "for the system, and <id> is the id of this content in that system.",
                example = "urn:article:1")
        public URI contentUri;

        @JsonProperty
        @ApiModelProperty(value = "Element name", example = "Basic physics", notes = "For performance reasons, this " +
                "name is for informational purposes only. To get a translated name, please fetch the resolved resource using its rest resource.")
        public String name;

        @JsonProperty
        @ApiModelProperty(value = "Parent elements of the resolved element", notes = "The first element is the parent, the second is the grandparent, etc.")
        public URI[] parents = new URI[0];
    }
}
