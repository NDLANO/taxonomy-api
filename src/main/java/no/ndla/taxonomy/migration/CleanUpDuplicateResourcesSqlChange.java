/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.migration;

import java.sql.ResultSet;
import java.sql.SQLException;
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

public class CleanUpDuplicateResourcesSqlChange implements CustomSqlChange {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    private JdbcConnection connection;

    @Override
    public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
        connection = (JdbcConnection) database.getConnection();

        try {
            connection.setAutoCommit(false);
        } catch (DatabaseException e) {
            throw new CustomChangeException("Error starting transaction. No changes made. Error:" + e.getMessage(), e);
        }

        try {
            try (var contentUrisSharedByMultipleResources = connection.prepareStatement(
                    "select content_uri from (select count(id) as antall, content_uri from node where node_type = 'RESOURCE' group by content_uri order by antall desc) as duplicates where antall > 1")) {
                final var resourceListQuery = contentUrisSharedByMultipleResources.executeQuery();

                while (resourceListQuery.next()) {
                    final var contentUri = resourceListQuery.getString(1);

                    if (contentUri != null) {
                        // ignoring resurces with null in contenturi
                        cleanUpResources(contentUri);
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

    private void cleanUpResources(String contentUri) throws DatabaseException, SQLException {
        logger.info(String.format("Cleaning up nodes with content_uri %s", contentUri));
        ResultSet result = connection
                .prepareStatement(String.format("SELECT id from node where content_uri = '%s'", contentUri))
                .executeQuery();
        // First one wins
        result.next();
        final var winning = result.getInt(1);
        // Delete the others
        while (result.next()) {
            final var id = result.getInt(1);
            // reconnect winning resource in node_connection to replace the one to delete
            reconnectConnections(winning, id);
            // delete remaining connections
            connection
                    .prepareStatement(String.format("delete from node_connection where child_id = %s", id))
                    .executeUpdate();
            // delete the node
            connection
                    .prepareStatement(String.format("delete from resource_resource_type where resource_id = %s", id))
                    .executeUpdate();
            connection
                    .prepareStatement(String.format("delete from node where id = %s", id))
                    .executeUpdate();
        }
    }

    private void reconnectConnections(int winning, int loosing) throws DatabaseException, SQLException {
        // Get all parents to winning node
        ResultSet parents = connection
                .prepareStatement(String.format("select parent_id from node_connection where child_id = %s", winning))
                .executeQuery();
        if (parents != null) {
            while (parents.next()) {
                final var parent = parents.getInt(1);
                // Check if loosing node has same parent as winning node
                ResultSet connections = connection
                        .prepareStatement(String.format(
                                "select count(*) from node_connection where parent_id = %s and child_id = %s",
                                parent, loosing))
                        .executeQuery();
                connections.next();
                final var count = connections.getInt(1);
                if (count == 0) {
                    // No shared parent, put winning node into the position of the loosing node
                    connection
                            .prepareStatement(String.format(
                                    "update node_connection set child_id = %s where child_id = %s", winning, loosing))
                            .executeUpdate();
                } else {
                    // Shared parents, so it's safe to delete the connection to the loosing node
                    connection
                            .prepareStatement(String.format("delete from node_connection where child_id = %s", loosing))
                            .executeUpdate();
                }
            }
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "Cleaned up all duplicate resources";
    }

    @Override
    public void setUp() throws SetupException {}

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {}

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }
}
