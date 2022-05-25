/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.EntityWithPath;

import java.net.URI;

public abstract class VersionSchemaFetcher<TYPE extends EntityWithPath> extends VersionSchemaTask<TYPE> {
    protected TYPE type;
    protected URI publicId;
    protected boolean addCustomField;

    public void setType(TYPE type) {
        this.type = type;
    }

    public void setPublicId(URI publicId) {
        this.publicId = publicId;
    }

    public void setAddCustomField(boolean addCustomField) {
        this.addCustomField = addCustomField;
    }
}
