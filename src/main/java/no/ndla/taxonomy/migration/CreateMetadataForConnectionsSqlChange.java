/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class CreateMetadataForConnectionsSqlChange implements CustomSqlChange {

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String schema;

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getSchema() {
        if (schema != null) {
            return schema + ".";
        }
        return "";
    }

    @Override
    public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
        JdbcConnection connection = (JdbcConnection) database.getConnection();
        try {
            populateMetadataForTable(connection, "node_connection", "child_id", "node");
            populateMetadataForTable(connection, "node_resource", "resource_id", "resource");
        } catch (SQLException | DatabaseException exception) {
            // Should just fail the migration. No updates are run before returning from this method,
            // so no damage is done
            logger.info("Migration failed. Look into this", exception);
            throw new RuntimeException(exception);
        }
        return new SqlStatement[0];
    }

    private void populateMetadataForTable(JdbcConnection connection, String table, String childColumn, String childType)
            throws SQLException, DatabaseException {
        logger.info(String.format("Updating %s with metadata", table));
        ResultSet result = connection
                .prepareStatement(
                        String.format("SELECT id, %s from %s", childColumn, String.format("%s%s", getSchema(), table)))
                .executeQuery();
        if (result != null) {
            while (result.next()) {
                var connectionId = result.getInt(1);
                var childId = result.getInt(2);
                PreparedStatement getMetadataId = connection.prepareStatement(String.format(
                        "SELECT metadata_id from %s where id = ?", String.format("%s%s", getSchema(), childType)));
                getMetadataId.setInt(1, childId);
                ResultSet metadataIdRS = getMetadataId.executeQuery();
                metadataIdRS.next();
                int oldMetadataId = metadataIdRS.getInt(1);

                PreparedStatement getMetadata = connection.prepareStatement(String
                        .format("SELECT visible from %s where id = ?", String.format("%s%s", getSchema(), "metadata")));
                getMetadata.setInt(1, oldMetadataId);
                ResultSet metadataRs = getMetadata.executeQuery();
                metadataRs.next();
                boolean visible = metadataRs.getObject(1, Boolean.class);
                PreparedStatement insertMetadata = connection.prepareStatement(
                        String.format("insert into %s (visible, created_at) values (?, ?) returning id",
                                String.format("%s%s", getSchema(), "metadata")));
                insertMetadata.setBoolean(1, visible);
                insertMetadata.setTimestamp(2, Timestamp.from(Instant.now()));
                ResultSet resultSet = insertMetadata.executeQuery();
                resultSet.next();
                Integer newMetadataId = resultSet.getObject(1, Integer.class);

                PreparedStatement getGrepCode = connection
                        .prepareStatement(String.format("select grep_code_id from %s where metadata_id = ?",
                                String.format("%s%s", getSchema(), "metadata_grep_code")));
                getGrepCode.setInt(1, oldMetadataId);
                ResultSet grepCodesRs = getGrepCode.executeQuery();
                while (grepCodesRs.next()) {
                    int grepCodeId = grepCodesRs.getInt(1);
                    PreparedStatement insertGrepCode = connection
                            .prepareStatement(String.format("insert into %s (metadata_id, grep_code_id) values (?, ?)",
                                    String.format("%s%s", getSchema(), "metadata_grep_code")));
                    insertGrepCode.setInt(1, newMetadataId);
                    insertGrepCode.setInt(2, grepCodeId);
                    insertGrepCode.executeUpdate();
                }

                PreparedStatement getCustomFields = connection
                        .prepareStatement(String.format("select custom_field_id, value from %s where metadata_id = ?",
                                String.format("%s%s", getSchema(), "custom_field_value")));
                getCustomFields.setInt(1, oldMetadataId);
                ResultSet customFieldsRS = getCustomFields.executeQuery();
                while (customFieldsRS.next()) {
                    int customFieldId = customFieldsRS.getInt(1);
                    String value = customFieldsRS.getString(2);
                    PreparedStatement insertCustomField = connection.prepareStatement(
                            String.format("insert into %s (metadata_id, custom_field_id, value) values (?, ?, ?)",
                                    String.format("%s%s", getSchema(), "custom_field_value")));
                    insertCustomField.setInt(1, newMetadataId);
                    insertCustomField.setInt(2, customFieldId);
                    insertCustomField.setString(3, value);
                    insertCustomField.executeUpdate();
                }

                PreparedStatement updateTable = connection.prepareStatement(String.format(
                        "update %s set metadata_id = ? where id = ?", String.format("%s%s", getSchema(), table)));
                updateTable.setInt(1, newMetadataId);
                updateTable.setInt(2, connectionId);
                updateTable.executeUpdate();
            }
        }

    }

    @Override
    public String getConfirmationMessage() {
        return "Added metadata to connections from child";
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
