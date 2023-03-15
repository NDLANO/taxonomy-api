/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import no.ndla.taxonomy.domain.Version;
import no.ndla.taxonomy.repositories.VersionRepository;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Updates the Connection object with the correct schema, based on the tenant identifier
 */
@Component
public class VersionConnectionProvider extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {
    private final Map<String, DataSource> dataSourceMap = new HashMap<>();
    private final DataSource defaultDataSource;

    @Value("${spring.datasource.url:}")
    private String dataSourceUrl;

    @Value("${spring.datasource.username:}")
    private String dataSourceUsername;

    @Value("${spring.datasource.password:}")
    private String dataSourcePassword;

    @Value("${spring.datasource.hikari.schema:taxonomy_api}")
    private String defaultSchema;

    private String schemaFromHash(String hash) {
        if (hash != null)
            return String.format("%s%s", defaultSchema, "_" + hash);
        return defaultSchema;
    }

    @Autowired
    public VersionConnectionProvider(DataSource dataSource) {
        this.defaultDataSource = dataSource;
    }

    private DataSource getDataSource(String tenant) {
        var foundDataSource = dataSourceMap.get(tenant);
        if (foundDataSource != null) {
            return foundDataSource;
        }

        var config = new HikariConfig();
        config.setSchema(tenant);
        config.setJdbcUrl(dataSourceUrl);
        config.setUsername(dataSourceUsername);
        config.setPassword(dataSourcePassword);
        var newDataSource = new HikariDataSource(config);
        dataSourceMap.put(tenant, newDataSource);

        return newDataSource;
    }

    @Override
    protected DataSource selectAnyDataSource() {
        return defaultDataSource;
    }

    @Override
    protected DataSource selectDataSource(String versionSchemaName) {
        return getDataSource(versionSchemaName);
    }
}
