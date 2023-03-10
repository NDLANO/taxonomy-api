/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Updates the Connection object with the correct schema, based on the tenant identifier
 */
@Component
public class VersionConnectionProvider extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {
    private final DataSource defaultDataSource;
    private final VersionService versionService;
    private boolean initCompleted = false;
    private final Map<String, DataSource> dataSourceMap = new HashMap<>();

    @Value("${spring.datasource.hikari.schema:taxonomy_api}")
    private String defaultSchema;

    private void initDataSourceMap() {
        var storedVersions = versionService.getVersions();
        for (var version: storedVersions) {
            var schemaName = versionService.schemaFromHash(version.getHash());
            // FIXME: Create datasource for tenant
            dataSourceMap.put(schemaName, ds);

        }

    }

    @Autowired
    public VersionConnectionProvider(DataSource dataSource, VersionService versionService) {
        this.defaultDataSource = dataSource;
        this.versionService = versionService;
    }

    @Override
    protected DataSource selectAnyDataSource() {
        return defaultDataSource;
    }

    @Override
    protected DataSource selectDataSource(String versionSchemaName) {
        return dataSource;
    }

    @Override
    public Connection getConnection(String versionSchemaName) throws SQLException {
        final Connection connection = getAnyConnection();
        connection.setSchema(versionSchemaName);
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        connection.setSchema(defaultSchema);
        connection.close();
    }
}
