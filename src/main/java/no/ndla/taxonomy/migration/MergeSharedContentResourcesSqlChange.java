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
import java.util.ArrayList;

public class MergeSharedContentResourcesSqlChange implements CustomSqlChange {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    private JdbcConnection connection;

    private void mergeResourceRelations(int destinationId, int sourceId) throws CustomChangeException {
        try {
            try (var filtersQuery = connection.prepareStatement("SELECT id, filter_id FROM resource_filter rf1 WHERE resource_id = " + sourceId)) {
                final var filtersResult = filtersQuery.executeQuery();

                while (filtersResult.next()) {
                    final var connectionId = filtersResult.getInt(1);
                    final var filterId = filtersResult.getInt(2);

                    try (var filterExistsOnDestinationQuery = connection.prepareStatement("SELECT COUNT(*) FROM resource_filter WHERE filter_id = " + filterId + " AND resource_id = " + destinationId)) {
                        final var filterExistsResult = filterExistsOnDestinationQuery.executeQuery();
                        filterExistsResult.next();

                        final var existsCount = filterExistsResult.getInt(1);

                        if (existsCount == 0) {
                            try (final var updateQuery = connection.prepareStatement("UPDATE resource_filter SET resource_id = " + destinationId + " WHERE id = " + connectionId)) {
                                updateQuery.executeUpdate();
                            }
                        } else {
                            try (final var deleteQuery = connection.prepareStatement("DELETE FROM resource_filter WHERE id = " + connectionId)) {
                                deleteQuery.executeUpdate();
                            }
                        }
                    }
                }
            }

            try (var resourceTypeQuery = connection.prepareStatement("SELECT id, resource_type_id FROM resource_resource_type WHERE resource_id = " + sourceId)) {
                final var resourceTypeResult = resourceTypeQuery.executeQuery();

                while (resourceTypeResult.next()) {
                    final var connectionId = resourceTypeResult.getInt(1);
                    final var resourceTypeId = resourceTypeResult.getInt(2);

                    try (var existsOnDestinationQuery = connection.prepareStatement("SELECT COUNT(*) FROM resource_resource_type WHERE resource_type_id = " + resourceTypeId + " AND resource_id = " + destinationId)) {
                        final var existsOnDestinationResult = existsOnDestinationQuery.executeQuery();
                        existsOnDestinationResult.next();

                        final var existsCount = existsOnDestinationResult.getInt(1);

                        if (existsCount == 0) {
                            try (final var updateQuery = connection.prepareStatement("UPDATE resource_resource_type SET resource_id = " + destinationId + " WHERE id = " + connectionId)) {
                                updateQuery.executeUpdate();
                            }
                        } else {
                            try (final var deleteQuery = connection.prepareStatement("DELETE FROM resource_resource_type WHERE id = " + connectionId)) {
                                deleteQuery.executeUpdate();
                            }
                        }
                    }
                }
            }

            try (var resourceTypeQuery = connection.prepareStatement("SELECT id, resource_type_id FROM resource_resource_type WHERE resource_id = " + sourceId)) {
                final var resourceTypeResult = resourceTypeQuery.executeQuery();

                while (resourceTypeResult.next()) {
                    final var connectionId = resourceTypeResult.getInt(1);
                    final var resourceTypeId = resourceTypeResult.getInt(2);

                    try (var existsOnDestinationQuery = connection.prepareStatement("SELECT COUNT(*) FROM resource_resource_type WHERE resource_type_id = " + resourceTypeId + " AND resource_id = " + destinationId)) {
                        final var existsOnDestinationResult = existsOnDestinationQuery.executeQuery();
                        existsOnDestinationResult.next();

                        final var existsCount = existsOnDestinationResult.getInt(1);

                        if (existsCount == 0) {
                            try (final var updateQuery = connection.prepareStatement("UPDATE resource_resource_type SET resource_id = " + destinationId + " WHERE id = " + connectionId)) {
                                updateQuery.executeUpdate();
                            }
                        } else {
                            try (final var deleteQuery = connection.prepareStatement("DELETE FROM resource_resource_type WHERE id = " + connectionId)) {
                                deleteQuery.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (SQLException | DatabaseException e) {
            throw new CustomChangeException(e);
        }
    }

    private void mergeResources(int destinationResourceId) throws CustomChangeException {
        try (var sharedContentQuery = connection.prepareStatement("SELECT r1.id FROM resource r1 WHERE r1.content_uri = (SELECT content_uri FROM resource r2 WHERE r2.id = " + destinationResourceId + ") AND r1.id != " + destinationResourceId)) {
            final var sharedContentResult = sharedContentQuery.executeQuery();

            while (sharedContentResult.next()) {
                final var sourceResourceId = sharedContentResult.getInt(1);

                mergeResourceRelations(destinationResourceId, sourceResourceId);

                try (var updateConnectionsQuery = connection.prepareStatement("UPDATE topic_resource SET resource_id =  " + destinationResourceId + " WHERE resource_id = " + sourceResourceId)) {
                    updateConnectionsQuery.executeUpdate();
                }

                try (var removeTranslationsQuery = connection.prepareStatement("DELETE FROM resource_translation WHERE resource_id = " + sourceResourceId)) {
                    removeTranslationsQuery.executeUpdate();
                }

                try (var deleteResourceQuery = connection.prepareStatement("DELETE FROM resource WHERE id = " + sourceResourceId)) {
                    deleteResourceQuery.executeUpdate();
                }
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
            final var resourceIdsToMerge = new ArrayList<Integer>();

            try {
                try (var findSharedContentQuery = connection.prepareStatement("SELECT id FROM resource r1 WHERE r1.content_uri IN (SELECT content_uri FROM resource WHERE content_uri IS NOT NULL GROUP BY content_uri HAVING COUNT(id) > 1) ORDER BY id")) {
                    final var findSharedContentResult = findSharedContentQuery.executeQuery();

                    while (findSharedContentResult.next()) {
                        resourceIdsToMerge.add(findSharedContentResult.getInt(1));
                    }
                }
            } catch (SQLException | DatabaseException e) {
                throw new CustomChangeException(e);
            }

            logger.info("Found " + resourceIdsToMerge.size() + " resources to merge into");


            var mergedResources = 0;
            for (var resourceId : resourceIdsToMerge) {
                mergeResources(resourceId);
                mergedResources++;

                if (mergedResources % 100 == 0) {
                    logger.info("Merged " + mergedResources + "/" + resourceIdsToMerge.size() + " resources");
                }
            }

            logger.info("Merged " + mergedResources + "/" + resourceIdsToMerge.size() + " resources");
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
