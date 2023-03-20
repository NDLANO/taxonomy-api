/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.service.DomainEntityHelperService;

import java.util.Optional;

public class Updater extends Task<DomainEntity> {
    private DomainEntityHelperService domainEntityHelperService;
    private DomainEntity element;

    public void setDomainEntityHelperService(DomainEntityHelperService domainEntityHelperService) {
        this.domainEntityHelperService = domainEntityHelperService;
    }

    public void setElement(DomainEntity element) {
        this.element = element;
    }

    public void setCleanUp(boolean cleanUp) {
        this.cleanUp = cleanUp;
    }

    @Override
    protected Optional<DomainEntity> execute() {
        return domainEntityHelperService.updateEntity(element, this.cleanUp);
    }

}
