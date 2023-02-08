/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.service.ChangelogService;
import no.ndla.taxonomy.service.DomainEntityHelperService;
import no.ndla.taxonomy.service.exceptions.EntityNotFoundException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Collectors;

public class Fetcher extends Task<DomainEntity> {
    private DomainEntityHelperService domainEntityHelperService;
    private URI publicId;
    private boolean addIsPublishing;

    public Fetcher() {
    }

    public void setDomainEntityHelperService(DomainEntityHelperService domainEntityHelperService) {
        this.domainEntityHelperService = domainEntityHelperService;
    }

    public void setPublicId(URI publicId) {
        this.publicId = publicId;
    }

    public void setAddIsPublishing(boolean addIsPublishing) {
        this.addIsPublishing = addIsPublishing;
    }

    @Override
    protected Optional<DomainEntity> execute() {
        return domainEntityHelperService.getProcessedEntityByPublicId(this.publicId, this.addIsPublishing,
                this.cleanUp);
    }
}
