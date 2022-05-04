/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Gets the database schema name from the version context to be used in the multi tenancy provider.
 */
@Component
public class VersionIdentifierResolver implements CurrentTenantIdentifierResolver {

    @Value("${spring.datasource.hikari.schema:PUBLIC}")
    private String defaultSchema;

    @Override
    public String resolveCurrentTenantIdentifier() {
        return Optional.ofNullable(VersionContext.getCurrentVersion()).orElse(defaultSchema);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
