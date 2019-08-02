package no.ndla.taxonomy.rest.v1;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.NotFoundException;
import no.ndla.taxonomy.rest.BadHttpRequestException;
import no.ndla.taxonomy.rest.NotFoundHttpRequestException;
import no.ndla.taxonomy.service.UrlResolverService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.net.URI;

import static org.apache.commons.lang3.StringUtils.isBlank;

@RestController
@RequestMapping("/v1/url")
@Transactional
public class UrlResolver {
    private final UrlResolverService urlResolverService;

    @Autowired
    public UrlResolver(UrlResolverService urlResolverService) {
        this.urlResolverService = urlResolverService;
    }

    public static String getPathMostCloselyMatchingContext(String context, String... urls) {
        String longestPrefix = "";
        String bestUrl = "";
        for (String possibleUrl : urls) {
            String commonPrefix = StringUtils.getCommonPrefix(context + "/", possibleUrl);
            if (commonPrefix.length() >= longestPrefix.length()) {
                bestUrl = possibleUrl;
                longestPrefix = commonPrefix;
            }
        }
        return bestUrl;
    }

    @GetMapping("/resolve")
    public ResolvedUrl resolve(@RequestParam String path, HttpServletResponse response) throws Exception {
        URI id = getId(path);

        final var resolvablePathEntities = urlResolverService.getResolvablePathEntitiesFromPublicId(id);

        ResolvedUrl returnedResolvedUrl = new ResolvedUrl();
        ResolvedUrl resolvedUrl = new ResolvedUrl();
        if (resolvablePathEntities.size() > 0) {
            for (final var entity : resolvablePathEntities) {
                resolvedUrl.id = entity.getPublicId();
                resolvedUrl.contentUri = entity.getContentUri();
                resolvedUrl.name = entity.getName();
                for (var foundPath : entity.getAllPaths()) {
                    resolvedUrl.path = getPathMostCloselyMatchingContext(path, resolvedUrl.path, foundPath);
                    if (resolvedUrl.path.equals(path)) {
                        break;
                    }
                }
            }
            returnedResolvedUrl = resolvedUrl;
        }

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

    @GetMapping("/mapping")
    @ApiOperation(value = "Returns path for an url or HTTP 404")
    public ResolvedOldUrl getTaxonomyPathForUrl(@ApiParam(value = "url in old rig except 'https://'", example = "ndla.no/nb/node/142542?fag=52253") @RequestParam String url) {
        String resolveOldUrl = urlResolverService.resolveUrl(url);
        if (resolveOldUrl != null) {
            ResolvedOldUrl resolvedOldUrl = new ResolvedOldUrl();
            resolvedOldUrl.path = resolveOldUrl;
            return resolvedOldUrl;
        } else {
            throw new NotFoundException(url);
        }
    }

    @PutMapping("/mapping")
    @ApiOperation(value = "Inserts or updates a mapping from url to nodeId and optionally subjectId")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void putTaxonomyNodeAndSubjectForOldUrl(@RequestBody UrlMapping urlMapping) {
        try {
            urlResolverService.putUrlMapping(urlMapping.url, URI.create(urlMapping.nodeId), URI.create(urlMapping.subjectId));
        } catch (IllegalArgumentException ex) {
            throw new BadHttpRequestException(ex.getMessage());
        } catch (UrlResolverService.NodeIdNotFoundExeption ex) {
            throw new NotFoundHttpRequestException(ex.getMessage());
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


    public static class UrlMapping {
        @ApiModelProperty(value = "URL for resource in old system", example = "ndla.no/nb/node/183926?fag=127013")
        @JsonProperty
        public String url;

        @ApiModelProperty(value = "Node URN for resource in new system", example = "urn:topic:1:183926")
        @JsonProperty
        public String nodeId;

        @ApiModelProperty(value = "Subject URN for resource in new system (optional)", example = "urn:subject:5")
        @JsonProperty
        public String subjectId;

        @JsonCreator
        public UrlMapping() {
        }

        public UrlMapping(String url, URI nodeId, URI subjectId) {
            this.url = url;
            this.nodeId = nodeId.toString();
            this.subjectId = subjectId.toString();
        }
    }
}
