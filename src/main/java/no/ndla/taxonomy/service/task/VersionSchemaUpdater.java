/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

public abstract class VersionSchemaUpdater<TYPE> extends VersionSchemaTask<TYPE> {
    protected TYPE type;

    public void setType(TYPE type) {
        this.type = type;
    }
}
