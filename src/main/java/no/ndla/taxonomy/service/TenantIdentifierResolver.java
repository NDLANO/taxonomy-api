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

@Component
// @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    @Value("${spring.datasource.hikari.schema:public}") // Default value used in test.
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
