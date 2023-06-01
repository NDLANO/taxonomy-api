/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;
import no.ndla.taxonomy.service.dtos.MetadataApiEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

public class CopyDataFromMetadataServiceSqlChange implements CustomSqlChange {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final RestTemplate restTemplate = new RestTemplate();
    private final HashMap<String, Integer> grepCodes = new HashMap<>();
    private final HashMap<String, Integer> customFields = new HashMap<>();

    @Override
    public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
        JdbcConnection connection = (JdbcConnection) database.getConnection();
        try {
            // If code is not run in k8s this throws IO exception
            String baseUrl = "http://taxonomy-metadata/v1/taxonomy_entities/";
            restTemplate.getForEntity(baseUrl + "?key=", MetadataApiEntity[].class);

            populateMetadataForTable(connection, baseUrl, "node");
            populateMetadataForTable(connection, baseUrl, "resource");
        } catch (ResourceAccessException resourceAccessException) {
            // can't connect to metadata-service
        } catch (SQLException | DatabaseException exception) {
            // Should just fail the migration. No updates are run before returning from this method,
            // so no damage is done
            throw new RuntimeException(exception);
        }
        return new SqlStatement[0];
    }

    private void populateMetadataForTable(JdbcConnection connection, String baseUrl, String table)
            throws SQLException, DatabaseException {
        logger.info(String.format("Updating %s with metadata", table));
        ResultSet result = connection
                .prepareStatement(String.format("SELECT id, public_id from %s", table))
                .executeQuery();
        Map<String, Integer> batch = new HashMap<>();
        if (result != null) {
            while (result.next()) {
                batch.put(result.getString(2), result.getInt(1));
                if (batch.size() == 100 || result.isLast()) {
                    String publicIds = String.join(",", batch.keySet());
                    MetadataApiEntity[] entities = restTemplate
                            .getForEntity(baseUrl + "?publicIds=" + publicIds, MetadataApiEntity[].class)
                            .getBody();
                    assert entities != null;
                    for (MetadataApiEntity entity : entities) {
                        assert entity != null;
                        PreparedStatement insertMetadata = connection.prepareStatement(
                                "insert into metadata (visible, created_at) values (?, ?) returning id");
                        insertMetadata.setBoolean(1, entity.isVisible().orElse(true));
                        insertMetadata.setTimestamp(2, Timestamp.from(Instant.now()));
                        ResultSet resultSet = insertMetadata.executeQuery();
                        resultSet.next();
                        Integer metadataId = resultSet.getObject(1, Integer.class);

                        PreparedStatement updateTable = connection.prepareStatement(
                                String.format("update %s set metadata_id = ? where id = ?", table));
                        updateTable.setInt(1, metadataId);
                        updateTable.setInt(2, batch.get(entity.getPublicId()));
                        updateTable.executeUpdate();

                        if (entity.getCompetenceAims().isPresent()) {
                            for (MetadataApiEntity.CompetenceAim competanceAim :
                                    entity.getCompetenceAims().get()) {
                                Integer grepId = createOrGetGrepCodeId(competanceAim.getCode(), connection);
                                PreparedStatement insertGrepCode = connection.prepareStatement(
                                        "insert into metadata_grep_code (metadata_id, grep_code_id) values (?, ?)");
                                insertGrepCode.setInt(1, metadataId);
                                insertGrepCode.setInt(2, grepId);
                                insertGrepCode.executeUpdate();
                            }
                        }

                        if (entity.getCustomFields().isPresent()) {
                            Map<String, String> customFieldMap =
                                    entity.getCustomFields().get();
                            for (String customField : customFieldMap.keySet()) {
                                Integer customFieldId = createOrGetCustomFieldId(customField, connection);
                                String value = customFieldMap.get(customField);
                                PreparedStatement statement = connection.prepareStatement(
                                        "insert into custom_field_value (metadata_id, custom_field_id, value) values (?, ?, ?)");
                                statement.setInt(1, metadataId);
                                statement.setInt(2, customFieldId);
                                statement.setString(3, value);
                                statement.executeUpdate();
                            }
                        }
                    }
                    batch.clear();
                }
            }
        }
    }

    private Integer createOrGetCustomFieldId(String customField, JdbcConnection connection)
            throws SQLException, DatabaseException {
        if (customFields.containsKey(customField)) {
            return customFields.get(customField);
        }
        PreparedStatement statement =
                connection.prepareStatement("insert into custom_field (key, created_at) values (?, ?) returning id");
        statement.setString(1, customField);
        statement.setTimestamp(2, Timestamp.from(Instant.now()));
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();
        Integer customFieldId = resultSet.getInt(1);
        customFields.put(customField, customFieldId);
        return customFieldId;
    }

    private Integer createOrGetGrepCodeId(String code, JdbcConnection connection)
            throws DatabaseException, SQLException {
        if (grepCodes.containsKey(code)) {
            return grepCodes.get(code);
        }
        PreparedStatement statement =
                connection.prepareStatement("insert into grep_code (code, created_at) values (?, ?) returning id");
        statement.setString(1, code);
        statement.setTimestamp(2, Timestamp.from(Instant.now()));
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();
        Integer grepId = resultSet.getInt(1);
        grepCodes.put(code, grepId);
        return grepId;
    }

    @Override
    public String getConfirmationMessage() {
        return "Migrated all metadata";
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
