/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.EntityWithPath;
import no.ndla.taxonomy.service.VersionContext;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.Callable;

public abstract class VersionSchemaTask<TYPE extends EntityWithPath> implements Callable<TYPE> {

    protected Optional<URI> sourceId;
    protected URI targetId;
    private String version;

    public void setSourceId(Optional<URI> sourceId) {
        this.sourceId = sourceId;
    }

    public void setTargetId(URI targetId) {
        this.targetId = targetId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public final TYPE call() throws Exception {
        VersionContext.setCurrentVersion(this.version);
        return callInternal().get();
    }

    protected abstract Optional<TYPE> callInternal();
}
