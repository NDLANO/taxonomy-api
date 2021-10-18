/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.ndla.taxonomy.domain.exceptions.NotFoundException;
import no.ndla.taxonomy.rest.NotFoundHttpResponseException;
import no.ndla.taxonomy.service.UrlResolverService;
import no.ndla.taxonomy.service.dtos.ResolvedUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.net.URI;

@RestController
@RequestMapping("/v1/url")
@Transactional
public class UrlResolver {
    private final UrlResolverService urlResolverService;

    @Autowired
    public UrlResolver(UrlResolverService urlResolverService) {
        this.urlResolverService = urlResolverService;
    }

    @GetMapping("/resolve")
    public ResolvedUrl resolve(@RequestParam String path) {
        return urlResolverService
                .resolveUrl(path)
                .orElseThrow(
                        () -> new NotFoundHttpResponseException("Element with path was not found"));
    }

    @GetMapping("/mapping")
    @ApiOperation(value = "Returns path for an url or HTTP 404")
    public ResolvedOldUrl getTaxonomyPathForUrl(
            @ApiParam(
                            value = "url in old rig except 'https://'",
                            example = "ndla.no/nb/node/142542?fag=52253")
                    @RequestParam
                    String url) {
        ResolvedOldUrl resolvedOldUrl = new ResolvedOldUrl();
        resolvedOldUrl.path =
                urlResolverService.resolveOldUrl(url).orElseThrow(() -> new NotFoundException(url));
        return resolvedOldUrl;
    }

    @PutMapping("/mapping")
    @ApiOperation(
            value = "Inserts or updates a mapping from url to nodeId and optionally subjectId")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAXONOMY_WRITE')")
    public void putTaxonomyNodeAndSubjectForOldUrl(@RequestBody UrlMapping urlMapping) {
        try {
            urlResolverService.putUrlMapping(
                    urlMapping.url,
                    URI.create(urlMapping.nodeId),
                    URI.create(urlMapping.subjectId));
        } catch (UrlResolverService.NodeIdNotFoundExeption ex) {
            throw new NotFoundHttpResponseException(ex.getMessage());
        }
    }

    public static class ResolvedOldUrl {
        @JsonProperty
        @ApiModelProperty(
                value = "URL path for resource",
                example = "'/subject:1/topic:12/resource:12'")
        public String path;
    }

    public static class UrlMapping {
        @ApiModelProperty(
                value = "URL for resource in old system",
                example = "ndla.no/nb/node/183926?fag=127013")
        @JsonProperty
        public String url;

        @ApiModelProperty(
                value = "Node URN for resource in new system",
                example = "urn:topic:1:183926")
        @JsonProperty
        public String nodeId;

        @ApiModelProperty(
                value = "Subject URN for resource in new system (optional)",
                example = "urn:subject:5")
        @JsonProperty
        public String subjectId;

        @JsonCreator
        public UrlMapping() {}

        public UrlMapping(String url, URI nodeId, URI subjectId) {
            this.url = url;
            this.nodeId = nodeId.toString();
            this.subjectId = subjectId.toString();
        }
    }
}
