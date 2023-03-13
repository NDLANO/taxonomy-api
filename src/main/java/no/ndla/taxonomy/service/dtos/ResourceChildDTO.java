/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodeConnection;

@Schema(name = "NodeChild")
public class ResourceChildDTO extends EntityWithPathChildDTO {
    public ResourceChildDTO() {

    }

    public ResourceChildDTO(Node node, NodeConnection nodeConnection, String languageCode) {
        super(node, nodeConnection, languageCode);
    }

}
