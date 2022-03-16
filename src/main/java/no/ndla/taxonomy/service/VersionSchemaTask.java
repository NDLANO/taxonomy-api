/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import java.util.concurrent.Callable;

public abstract class VersionSchemaTask<TYPE> implements Callable<TYPE> {
    private String version;
    protected URI publicId;
    protected TYPE type;

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPublicId(URI publicId) {
        this.publicId = publicId;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    @Override
    public final TYPE call() throws Exception {
        VersionContext.setCurrentVersion(this.version);
        return callInternal();
    }

    protected abstract TYPE callInternal();
}