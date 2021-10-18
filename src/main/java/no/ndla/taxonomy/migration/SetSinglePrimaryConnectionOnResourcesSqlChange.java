/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.migration;

import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class SetSinglePrimaryConnectionOnResourcesSqlChange implements CustomSqlChange {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    private JdbcConnection connection;

    private void setRandomPrimary(int resourceId) throws CustomChangeException {
        try (var query = connection
                .prepareStatement("SELECT id FROM topic_resource WHERE resource_id = " + resourceId + " LIMIT 1")) {
            logger.info("Setting random primary for resource " + resourceId);

            final var result = query.executeQuery();

            if (!result.next()) {
                throw new CustomChangeException("No results found when trying to set random primary connection");
            }

            final var connectionIdToUpdate = result.getInt(1);

            try (var updateQuery = connection.prepareStatement(
                    "UPDATE topic_resource SET is_primary = TRUE WHERE id = " + connectionIdToUpdate)) {
                updateQuery.executeUpdate();
            }
        } catch (SQLException | DatabaseException e) {
            throw new CustomChangeException(e);
        }
    }

    private void keepRandomPrimary(int resourceId) throws CustomChangeException {
        logger.info("Selecting random primary from multiple primaries on resource " + resourceId);
        try (var query = connection.prepareStatement(
                "SELECT id FROM topic_resource WHERE resource_id = " + resourceId + " AND is_primary = TRUE LIMIT 1")) {
            final var result = query.executeQuery();

            if (!result.next()) {
                throw new CustomChangeException("No results found when trying to set random primary connection");
            }

            final var connectionIdToKeep = result.getInt(1);

            try (var updateQuery = connection
                    .prepareStatement("UPDATE topic_resource SET is_primary = FALSE WHERE id != " + connectionIdToKeep
                            + " AND resource_id = " + resourceId)) {
                updateQuery.executeUpdate();
            }
        } catch (SQLException | DatabaseException e) {
            throw new CustomChangeException(e);
        }
    }

    @Override
    public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
        connection = (JdbcConnection) database.getConnection();

        try {
            connection.setAutoCommit(false);
        } catch (DatabaseException e) {
            throw new CustomChangeException("Error starting transaction. No changes made. Error:" + e.getMessage(), e);
        }

        try {
            try (var resourcesWithOtherThanOnePrimaryConnectionQuery = connection.prepareStatement(
                    "SELECT tr1.resource_id, (SELECT COUNT(*) FROM topic_resource tr2 WHERE is_primary = TRUE AND tr2.resource_id = tr1.resource_id) FROM topic_resource tr1 GROUP BY tr1.resource_id HAVING (SELECT COUNT(*) FROM topic_resource tr2 WHERE is_primary = TRUE AND tr2.resource_id = tr1.resource_id) != 1")) {
                final var resourceListQuery = resourcesWithOtherThanOnePrimaryConnectionQuery.executeQuery();

                while (resourceListQuery.next()) {
                    final var resourceId = resourceListQuery.getInt(1);
                    final var primaryCount = resourceListQuery.getInt(2);

                    if (primaryCount == 0) {
                        setRandomPrimary(resourceId);
                    } else if (primaryCount > 1) {
                        keepRandomPrimary(resourceId);
                    }
                }
            } catch (DatabaseException | SQLException exception) {
                throw new CustomChangeException(exception);
            }
        } catch (RuntimeException | CustomChangeException exception) {
            try {
                connection.rollback();
            } catch (DatabaseException ignored) {
            }

            throw exception;
        }

        return new SqlStatement[0];
    }

    @Override
    public String getConfirmationMessage() {
        return "Merged all shared resources";
    }

    @Override
    public void setUp() throws SetupException {
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
    }

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }
}
