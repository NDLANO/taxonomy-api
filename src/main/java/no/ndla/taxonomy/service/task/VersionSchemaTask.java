/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.service.VersionContext;

import java.net.URI;
import java.util.concurrent.Callable;

public abstract class VersionSchemaTask<TYPE> implements Callable<TYPE> {
    private String version;

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public final TYPE call() throws Exception {
        VersionContext.setCurrentVersion(this.version);
        return callInternal();
    }

    protected abstract TYPE callInternal();
}
