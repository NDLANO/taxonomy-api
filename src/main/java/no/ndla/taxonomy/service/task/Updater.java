/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.service.ChangelogService;

import java.util.Optional;

public class Updater extends Task<DomainEntity> {
    private ChangelogService changelogService;
    private DomainEntity element;

    public void setChangelogService(ChangelogService changelogService) {
        this.changelogService = changelogService;
    }

    public void setElement(DomainEntity element) {
        this.element = element;
    }

    public void setCleanUp(boolean cleanUp) {
        this.cleanUp = cleanUp;
    }

    @Override
    protected Optional<DomainEntity> execute() {
        if (element instanceof Node) {
            return changelogService.updateNode((Node) element, this.cleanUp);
        }
        if (element instanceof NodeConnection) {
            return changelogService.updateNodeConnection((NodeConnection) element, this.cleanUp);
        }
        throw new IllegalArgumentException("Wrong type of element to update: " + element.getEntityName());
    }

}
