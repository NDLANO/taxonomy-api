/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1.dtos.queries;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.ResourceResourceType;
import no.ndla.taxonomy.domain.ResourceTranslation;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

@Schema(name = "QueryResourceIndexDocument")
public class ResourceIndexDocument {
    @JsonProperty
    @Schema(description = "Resource id", example = "urn:resource:12")
    public URI id;

    @JsonProperty
    @Schema(description = "Resource name", example = "Basic physics")
    public String name;

    @JsonProperty
    @Schema(description = "Resource type(s)", example = "[{\"id\":\"urn:resourcetype:learningPath\", \"name\":\"Learning path\"}]")
    public Set<ResourceTypeIndexDocument> resourceTypes;

    @JsonProperty
    @Schema(description = "The ID of this resource in the system where the content is stored. This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier "
            + "for the system, and <id> is the id of this content in that system.", example = "urn:article:1")
    public URI contentUri;

    @JsonProperty
    @Schema(description = "URL path for resource", example = "'/subject:1/topic:12/resource:12'")
    public String path;

    @JsonProperty
    @Schema(description = "All paths leading to this resource", example = "[\"/subject:1/topic:12/resource:12\", \"/subject:2/topic:13/resource:12\"]")
    public Set<String> paths;

    public ResourceIndexDocument() {
    }

    public ResourceIndexDocument(Resource resource, String languageCode) {
        this.id = resource.getPublicId();
        this.name = resource.getTranslation(languageCode).map(ResourceTranslation::getName).orElse(resource.getName());

        this.resourceTypes = resource.getResourceResourceTypes().stream().map(ResourceResourceType::getResourceType)
                .map(resourceType -> new ResourceTypeIndexDocument(resourceType, languageCode))
                .collect(Collectors.toSet());

        this.contentUri = resource.getContentUri();
        this.path = resource.getPrimaryPath().orElse(null);
        this.paths = resource.getAllPaths();
    }
}
