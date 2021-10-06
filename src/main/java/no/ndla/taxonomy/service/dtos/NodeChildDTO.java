/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 - 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import io.swagger.annotations.ApiModel;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;

@ApiModel("NodeChild")
public class NodeChildDTO extends EntityWithPathChildDTO {
    public NodeChildDTO() {

    }

    public NodeChildDTO(Node node, NodeConnection nodeConnection, String languageCode) {
        super(node, nodeConnection, languageCode);
    }

}
