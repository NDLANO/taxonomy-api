/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import java.util.Optional;
import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.service.NodeService;

public class Deleter extends Task<DomainEntity> {
    private NodeService nodeService;

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @Override
    protected Optional<DomainEntity> execute() {
        return nodeService.disconnectAllInvisibleNodes();
    }
}
