/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.ndla.taxonomy.domain.Node;

import java.util.HashSet;
import java.util.Set;

@ApiModel("ResourceWithParents")
public class ResourceWithParentsDTO extends ResourceDTO {
    @JsonProperty
    @ApiModelProperty(value = "Parent topology nodes and whether or not connection type is primary", example = "["
            + "{\"id\": \"urn:topic:1:181900\"," + "\"name\": \"I dyrehagen\","
            + "\"contentUri\": \"urn:article:6662\"," + "\"path\": \"/subject:2/topic:1:181900\","
            + "\"primary\": \"true\"}]")
    private Set<NodeWithResourceConnectionDTO> parents = new HashSet<>();

    public ResourceWithParentsDTO() {
        super();
    }

    public ResourceWithParentsDTO(Node resource, String languageCode) {
        super(resource, languageCode);

        resource.getResourceChildren().stream()
                .map(nodeResource -> new NodeWithResourceConnectionDTO(nodeResource, languageCode))
                .forEach(parents::add);
    }

    public Set<NodeWithResourceConnectionDTO> getParents() {
        return parents;
    }

    public Set<NodeWithResourceConnectionDTO> getParentTopics() {
        return getParents();
    }
}
