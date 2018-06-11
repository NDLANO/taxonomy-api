package no.ndla.taxonomy.rest.v1;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.NotFoundException;
import no.ndla.taxonomy.service.UrlResolverService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.net.URI;
import java.util.Collections;

import static no.ndla.taxonomy.jdbc.QueryUtils.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

@RestController
@RequestMapping("/v1/url")
@Transactional
public class UrlResolver {

    private static final String RESOLVE_URL_QUERY = getQuery("resolve_url");

    private JdbcTemplate jdbcTemplate;
    private UrlResolverService urlResolverService;

    @Autowired
    public UrlResolver(JdbcTemplate jdbcTemplate, UrlResolverService urlResolverService) {
        this.jdbcTemplate = jdbcTemplate;
        this.urlResolverService = urlResolverService;
    }

    public static String getPathMostCloselyMatchingContext(String context, String... urls) {
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

    @GetMapping("/resolve")
    @PreAuthorize("hasAuthority('READONLY')")
    public ResolvedUrl resolve(@RequestParam String path, HttpServletResponse response) throws Exception {
        URI id = getId(path);

        ResolvedUrl returnedResolvedUrl = jdbcTemplate.query(RESOLVE_URL_QUERY, setQueryParameters(Collections.singletonList(id.toString())),
                resultSet -> {
                    ResolvedUrl resolvedUrl = new ResolvedUrl();
                    while (resultSet.next()) {
                        resolvedUrl.id = getURI(resultSet, "public_id");
                        resolvedUrl.contentUri = getURI(resultSet, "content_uri");
                        resolvedUrl.name = resultSet.getString("name");
                        resolvedUrl.path = getPathMostCloselyMatchingContext(path, resolvedUrl.path, resultSet.getString("resource_path"));
                        if (resolvedUrl.path.equals(path)) {
                            return resolvedUrl;
                        }
                    }
                    return resolvedUrl;
                }
        );
        if (isBlank(returnedResolvedUrl.path)) {
            throw new NotFoundException(path);
        }

        if (!returnedResolvedUrl.path.equals(path)) {
            response.sendRedirect(returnedResolvedUrl.path);
            return null;
        }

        returnedResolvedUrl.parents = getParents(returnedResolvedUrl.path);
        return returnedResolvedUrl;
    }


    private URI getId(String url) {
        String[] parts = url.split("/");
        return URI.create("urn:" + parts[parts.length - 1]);
    }

    private URI[] getParents(String path) {
        String[] pathElements = path.split("/");
        if (pathElements.length < 2) return new URI[0];

        URI[] result = new URI[pathElements.length - 2];
        for (int i = 1; i < pathElements.length - 1; i++) {
            String pathElement = pathElements[i];
            URI parent = URI.create("urn:" + pathElement);
            result[result.length - i] = parent;
        }
        return result;
    }

    @GetMapping("/resolveOldUrl")
    @PreAuthorize("hasAuthority('READONLY')")
    public ResolvedOldUrl resolveOldUrl(@RequestParam String oldUrl) {
        String resolveOldUrl = urlResolverService.resolveOldUrl(oldUrl);
        if (resolveOldUrl != null) {
            ResolvedOldUrl resolvedOldUrl = new ResolvedOldUrl();
            resolvedOldUrl.path = resolveOldUrl;
            return resolvedOldUrl;
        } else {
            throw new NotFoundException(oldUrl);
        }
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

        @JsonProperty
        @ApiModelProperty(value = "URL path for resource", example = "'/subject:1/topic:12/resource:12'")
        public String path;
    }

    public static class ResolvedOldUrl {
        @JsonProperty
        @ApiModelProperty(value = "URL path for resource", example = "'/subject:1/topic:12/resource:12'")
        public String path;
    }


}
