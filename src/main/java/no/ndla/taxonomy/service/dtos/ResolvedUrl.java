/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import java.util.List;

public class ResolvedUrl {
    @ApiModelProperty(value = "ID of the element referred to by the given path", example = "urn:resource:1")
    private URI id;

    @JsonProperty
    @ApiModelProperty(value = "The ID of this element in the system where the content is stored. ",
            notes = "This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier " +
                    "for the system, and <id> is the id of this content in that system.",
            example = "urn:article:1")
    private URI contentUri;

    @JsonProperty
    @ApiModelProperty(value = "Element name", example = "Basic physics", notes = "For performance reasons, this " +
            "name is for informational purposes only. To get a translated name, please fetch the resolved resource using its rest resource.")
    private String name;

    @JsonProperty
    @ApiModelProperty(value = "Parent elements of the resolved element", notes = "The first element is the parent, the second is the grandparent, etc.")
    private List<URI> parents;

    @JsonProperty
    @ApiModelProperty(value = "URL path for resource", example = "'/subject:1/topic:12/resource:12'")
    private String path;

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    public URI getContentUri() {
        return contentUri;
    }

    public void setContentUri(URI contentUri) {
        this.contentUri = contentUri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<URI> getParents() {
        return parents;
    }

    public void setParents(List<URI> parents) {
        this.parents = parents;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
