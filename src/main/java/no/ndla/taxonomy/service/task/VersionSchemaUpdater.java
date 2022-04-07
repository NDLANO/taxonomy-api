/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.task;

import no.ndla.taxonomy.domain.*;

public abstract class VersionSchemaUpdater<TYPE extends EntityWithPath> extends VersionSchemaTask<TYPE> {
    protected TYPE element;

    public void setElement(TYPE element) {
        this.element = element;
    }

    protected void mergeMetadata(TYPE present, Metadata metadata) {
        Metadata presentMetadata = present.getMetadata();
        presentMetadata.setVisible(metadata.isVisible());
        for (GrepCode grepCode : metadata.getGrepCodes()) {
            if (!presentMetadata.getGrepCodes().contains(grepCode)) {
                presentMetadata.addGrepCode(grepCode);
            }
        }
        for (CustomFieldValue customFieldValue : metadata.getCustomFieldValues()) {
            if (presentMetadata.getCustomFieldValues().contains(customFieldValue)) {
                presentMetadata.addCustomFieldValue(customFieldValue);
            }
        }
    }
}
