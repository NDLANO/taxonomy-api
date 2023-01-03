/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.Node;

@Schema(name = "Node")
public class NodeDTO extends EntityWithPathDTO {
    public NodeDTO() {

    }

    public NodeDTO(Node node, String languageCode) {
        super(node, languageCode);
    }

}
