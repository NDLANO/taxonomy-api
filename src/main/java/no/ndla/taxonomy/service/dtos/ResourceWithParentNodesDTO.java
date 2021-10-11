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
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.service.InjectMetadata;

import java.util.HashSet;
import java.util.Set;

@ApiModel("ResourceWithParentNodes")
public class ResourceWithParentNodesDTO extends ResourceDTO {
    @JsonProperty
    @ApiModelProperty(value = "Parent topology nodes and whether or not connection type is primary",
            example = "[" +
                    "{\"id\": \"urn:topic:1:181900\"," +
                    "\"name\": \"I dyrehagen\"," +
                    "\"contentUri\": \"urn:article:6662\"," +
                    "\"path\": \"/subject:2/topic:1:181900\"," +
                    "\"primary\": \"true\"}]")
    @InjectMetadata
    private Set<NodeWithResourceConnectionDTO> parentNodes = new HashSet<>();

    public ResourceWithParentNodesDTO() {
        super();
    }

    public ResourceWithParentNodesDTO(Resource resource, String languageCode) {
        super(resource, languageCode);

        resource.getNodeResources().stream()
                .map(nodeResource -> new NodeWithResourceConnectionDTO(nodeResource, languageCode))
                .forEach(parentNodes::add);
    }

    public Set<NodeWithResourceConnectionDTO> getParentNodes() {
        return parentNodes;
    }
}
