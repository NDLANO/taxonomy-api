/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import io.swagger.annotations.ApiModel;
import no.ndla.taxonomy.domain.Node;

@ApiModel("Node")
public class NodeDTO extends EntityWithPathDTO {
    public NodeDTO() {

    }

    public NodeDTO(Node node, String languageCode) {
        super(node, languageCode);
    }

}
