/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.Node;

import java.util.HashSet;
import java.util.Set;

@Schema(name = "ResourceWithParents")
public class ResourceWithParentsDTO extends ResourceDTO {
    @JsonProperty
    @Schema(description = "Parent topology nodes and whether or not connection type is primary", example = "["
            + "{\"id\": \"urn:topic:1:181900\"," + "\"name\": \"I dyrehagen\","
            + "\"contentUri\": \"urn:article:6662\"," + "\"path\": \"/subject:2/topic:1:181900\","
            + "\"primary\": \"true\"}]")
    private Set<NodeWithResourceConnectionDTO> parents = new HashSet<>();

    public ResourceWithParentsDTO() {
    }

    public ResourceWithParentsDTO(Node resource, String languageCode) {
        super(resource, languageCode);

        resource.getParentNodeConnections().stream()
                .map(nodeResource -> new NodeWithResourceConnectionDTO(nodeResource, languageCode))
                .forEach(parents::add);
    }

    public Set<NodeWithResourceConnectionDTO> getParents() {
        return parents;
    }

}
