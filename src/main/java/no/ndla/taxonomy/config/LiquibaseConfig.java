/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.config;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
public class LiquibaseConfig implements InitializingBean, ResourceLoaderAware {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private DataSource dataSource;

    @Value("${spring.datasource.hikari.schema:public}") // Default value used in test.
    private String defaultSchema;

    @Autowired
    private LiquibaseProperties liquibaseProperties;

    private ResourceLoader resourceLoader;

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            List<String> schemas = new ArrayList<>();
            ResultSet resultSet = dataSource.getConnection().prepareStatement("SELECT hash FROM version")
                    .executeQuery();
            while (resultSet.next()) {
                schemas.add(String.format("%s_%s", defaultSchema, resultSet.getString(1)));
            }
            this.runOnAllSchemas(dataSource, schemas);
        } catch (SQLException exception) {
            // No version table
            logger.info("Failed to find version table in database. Does not run migration on alternative schemas");
        }
    }

    protected void runOnAllSchemas(DataSource dataSource, Collection<String> schemas) throws LiquibaseException {
        for (String schema : schemas) {
            logger.info("Initializing Liquibase for version " + schema);
            SpringLiquibase liquibase = this.getSpringLiquibase(dataSource, schema);
            liquibase.afterPropertiesSet();
            logger.info("Liquibase ran for version " + schema);
        }
    }

    protected SpringLiquibase getSpringLiquibase(DataSource dataSource, String schema) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setResourceLoader(getResourceLoader());
        liquibase.setDataSource(dataSource);
        liquibase.setDefaultSchema(schema);
        liquibase.setChangeLog(liquibaseProperties.getChangeLog());
        liquibase.setContexts(liquibaseProperties.getContexts());
        return liquibase;
    }

    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
