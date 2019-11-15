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
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;

import static java.util.UUID.randomUUID;

public class RemoveSharedTopicsSqlChange implements CustomSqlChange {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    private JdbcConnection connection;

    private void cloneTranslations(String type, int fromId, int toId) throws DatabaseException, SQLException {
        final String tableName;
        final String relationColumnName;

        switch (type) {
            case "topic":
                tableName = "topic_translation";
                relationColumnName = "topic_id";
                break;
            case "resource":
                tableName = "resource_translation";
                relationColumnName = "resource_id";
                break;
            default:
                throw new IllegalArgumentException("Unknown translation type " + type);
        }

        try (var query = connection.prepareStatement("SELECT language_code, name FROM " + tableName + " WHERE " + relationColumnName + " = " + fromId); var translationQueryResult = query.executeQuery()) {
            while (translationQueryResult.next()) {
                final var languageCode = translationQueryResult.getString("language_code");
                final var name = translationQueryResult.getString("name");

                logger.debug("Adding translation for " + type + " " + fromId + " languagecode " + languageCode);

                try (var insertQuery = connection.prepareStatement("INSERT INTO " + tableName + " (" + relationColumnName + ", language_code, name) VALUES(?, ?, ?)")) {
                    insertQuery.setInt(1, toId);
                    insertQuery.setString(2, languageCode);
                    insertQuery.setString(3, name);

                    if (insertQuery.executeUpdate() != 1) {
                        throw new RuntimeException("Unable to add translation");
                    }
                }
            }
        }
    }

    private void cloneFilterConnections(String type, int fromId, int toId) throws DatabaseException, SQLException {
        final String tableName;
        final String relationColumnName;
        final String publicIdPrefix;

        switch (type) {
            case "topic":
                tableName = "topic_filter";
                relationColumnName = "topic_id";
                publicIdPrefix = "urn:topic-filter:";
                break;
            case "resource":
                tableName = "resource_filter";
                relationColumnName = "resource_id";
                publicIdPrefix = "urn:resource-filter:";
                break;
            default:
                throw new IllegalArgumentException("Unknown translation type " + type);
        }

        try (var filterQueryResult = connection.prepareStatement("SELECT filter_id, relevance_id FROM " + tableName + " WHERE " + relationColumnName + " = " + fromId).executeQuery()) {
            while (filterQueryResult.next()) {
                final var filterId = filterQueryResult.getInt(1);
                final var relevanceId = filterQueryResult.getInt(2);

                final var newPublicId = publicIdPrefix + randomUUID();

                logger.debug("Adding filter for " + type + " " + fromId + " filterId " + filterId + " relevanceId " + relevanceId);

                try (var insertQuery = connection.prepareStatement("INSERT INTO " + tableName + " (" + relationColumnName + ", public_id, filter_id, relevance_id) VALUES(?, ?, ?, ?)")) {
                    insertQuery.setInt(1, toId);
                    insertQuery.setString(2, newPublicId);
                    insertQuery.setInt(3, filterId);
                    insertQuery.setInt(4, relevanceId);

                    if (insertQuery.executeUpdate() != 1) {
                        throw new RuntimeException("Unable to add filter/relevance connection");
                    }
                }
            }
        }

    }

    private void cloneResourceTypeConnections(String type, int fromId, int toId) throws DatabaseException, SQLException {
        final String tableName;
        final String relationColumnName;
        final String publicIdPrefix;

        switch (type) {
            case "topic":
                tableName = "topic_resource_type";
                relationColumnName = "topic_id";
                publicIdPrefix = "urn:topic-resourcetype:";
                break;
            case "resource":
                tableName = "resource_resource_type";
                relationColumnName = "resource_id";
                publicIdPrefix = "urn:resource-resourcetype:";
                break;
            default:
                throw new IllegalArgumentException("Unknown translation type " + type);
        }

        try (var filterQueryResult = connection.prepareStatement("SELECT resource_type_id FROM " + tableName + " WHERE " + relationColumnName + " = " + fromId).executeQuery()) {
            while (filterQueryResult.next()) {
                final var resourceTypeId = filterQueryResult.getInt(1);

                final var newPublicId = publicIdPrefix + randomUUID();

                logger.debug("Adding resourceType for " + type + " " + fromId + " resourceTypeId " + resourceTypeId);

                try (var insertQuery = connection.prepareStatement("INSERT INTO " + tableName + " (" + relationColumnName + ", public_id, resource_type_id) VALUES(?, ?, ?)")) {
                    insertQuery.setInt(1, toId);
                    insertQuery.setString(2, newPublicId);
                    insertQuery.setInt(3, resourceTypeId);

                    if (insertQuery.executeUpdate() != 1) {
                        throw new RuntimeException("Unable to add resourceType connection");
                    }
                }
            }
        }

    }

    private void connectTopicResources(int fromId, int toId) throws DatabaseException, SQLException {
        try (var topicResourceQueryResult = connection.prepareStatement("SELECT resource_id, rank FROM topic_resource WHERE topic_id = " + fromId).executeQuery()) {
            while (topicResourceQueryResult.next()) {
                final var resourceId = topicResourceQueryResult.getInt(1);
                final var rank = topicResourceQueryResult.getInt(2);

                final var newPublicId = "urn:topic-resource:" + randomUUID();

                logger.debug("Connecting resource " + resourceId + " to topic " + toId);

                try (var insertQuery = connection.prepareStatement("INSERT INTO topic_resource (topic_id, public_id, resource_id, rank, is_primary) VALUES(?, ?, ?, ?, false)")) {
                    insertQuery.setInt(1, toId);
                    insertQuery.setString(2, newPublicId);
                    insertQuery.setInt(3, resourceId);
                    insertQuery.setInt(4, rank);

                    if (insertQuery.executeUpdate() != 1) {
                        throw new RuntimeException("Unable to add topicResource connection");
                    }
                }
            }
        }
    }

    private void connectSubtopics(int fromId, int toId) throws DatabaseException, SQLException {
        try (var topicSubtopicQueryResult = connection.prepareStatement("SELECT subtopic_id, rank FROM topic_subtopic WHERE topic_id = " + fromId).executeQuery()) {
            while (topicSubtopicQueryResult.next()) {
                final var subtopicId = topicSubtopicQueryResult.getInt(1);
                final var rank = topicSubtopicQueryResult.getInt(2);

                final var newPublicId = "urn:topic-subtopic:" + randomUUID();

                logger.debug("Connecting subtopic " + subtopicId + " to topic " + toId);

                try (var insertQuery = connection.prepareStatement("INSERT INTO topic_subtopic (topic_id, public_id, subtopic_id, rank, is_primary) VALUES(?, ?, ?, ?, false)")) {
                    insertQuery.setInt(1, toId);
                    insertQuery.setString(2, newPublicId);
                    insertQuery.setInt(3, subtopicId);
                    insertQuery.setInt(4, rank);

                    if (insertQuery.executeUpdate() != 1) {
                        throw new RuntimeException("Unable to add topicSubtopic connection");
                    }
                }
            }
        }
    }

    private String createNewPublicId(String oldPublicId) throws DatabaseException, SQLException {
        final var parts = oldPublicId.split(":");

        if (parts.length < 2) {
            throw new RuntimeException("Unknown format of publicId " + oldPublicId);
        }

        if (!parts[0].equalsIgnoreCase("urn")) {
            throw new RuntimeException("Unknown format of publicId " + oldPublicId);
        }

        final var type = parts[1];

        final var remainingParts = new StringBuilder();
        switch (parts.length) {
            case 4:
                for (int i = 2; i < parts.length; i++) {
                    remainingParts.append(":").append(parts[i]);
                }

                return createNewPublicId("urn:" + type + ":" + 0 + remainingParts.toString());
            case 5:
                if (!NumberUtils.isNumber(parts[2])) {
                    break;
                }

                var newSerial = Integer.parseInt(parts[2]);
                var newPublicId = "";

                for (int i = 3; i < parts.length; i++) {
                    remainingParts.append(":").append(parts[i]);
                }

                do {
                    newPublicId = "urn:" + type + ":" + ++newSerial + remainingParts.toString();
                } while (publicIdExists(newPublicId));

                return newPublicId;
        }

        return "urn:" + type + ":" + randomUUID();
    }

    private boolean publicIdExists(String newPublicId) throws DatabaseException, SQLException {
        try (var query = connection.prepareStatement("SELECT COUNT(*) FROM (SELECT public_id FROM topic UNION SELECT public_id FROM resource UNION SELECT public_id FROM resource) public_ids WHERE public_ids.public_id = ?")) {
            query.setString(1, newPublicId);
            final var resultSet = query.executeQuery();
            if (resultSet.next()) {
                return (resultSet.getInt(1) > 0);
            }
        }

        return false;
    }

    private int cloneTopic(int topicId) throws SQLException, DatabaseException {
        logger.info("Cloning topic with id " + topicId);

        try (var topicQueryResult = connection.prepareStatement("SELECT public_id, name, content_uri, context FROM topic WHERE id=" + topicId).executeQuery()) {

            if (!topicQueryResult.next()) {
                throw new RuntimeException("Topic not found");
            }

            final var publicId = topicQueryResult.getString("public_id");
            final var name = topicQueryResult.getString("name");
            final var content_uri = topicQueryResult.getString("content_uri");
            final var context = topicQueryResult.getBoolean("context");

            final var newPublicId = createNewPublicId(publicId);

            try (var newTopicStatement = connection.prepareStatement("INSERT INTO topic (public_id, name, content_uri, context) VALUES(?, ?, ?, ?) RETURNING id")) {
                newTopicStatement.setString(1, newPublicId);
                newTopicStatement.setString(2, name);
                newTopicStatement.setString(3, content_uri);
                newTopicStatement.setBoolean(4, context);

                final var resultSet = newTopicStatement.executeQuery();

                resultSet.next();

                final var newTopicId = resultSet.getInt("id");

                cloneTranslations("topic", topicId, newTopicId);
                cloneFilterConnections("topic", topicId, newTopicId);
                cloneResourceTypeConnections("topic", topicId, newTopicId);
                connectTopicResources(topicId, newTopicId);
                connectSubtopics(topicId, newTopicId);

                logger.info("Created new topic with ID " + newTopicId);


                return newTopicId;
            }
        }
    }

    private int cloneResource(int resourceId) throws SQLException, DatabaseException {
        logger.info("Cloning resource with id " + resourceId);

        try (var resourceQueryResult = connection.prepareStatement("SELECT public_id, name, content_uri FROM resource WHERE id = " + resourceId).executeQuery()) {

            if (!resourceQueryResult.next()) {
                throw new RuntimeException("Resource not found");
            }

            final var publicId = resourceQueryResult.getString("public_id");
            final var name = resourceQueryResult.getString("name");
            final var content_uri = resourceQueryResult.getString("content_uri");

            final var newPublicId = createNewPublicId(publicId);

            try (var newResourceStatement = connection.prepareStatement("INSERT INTO resource (public_id, name, content_uri) VALUES(?, ?, ?) RETURNING id")) {
                newResourceStatement.setString(1, newPublicId);
                newResourceStatement.setString(2, name);
                newResourceStatement.setString(3, content_uri);

                final var resultSet = newResourceStatement.executeQuery();

                resultSet.next();

                final var newResourceId = resultSet.getInt("id");

                cloneTranslations("resource", resourceId, newResourceId);
                cloneFilterConnections("resource", resourceId, newResourceId);
                cloneResourceTypeConnections("resource", resourceId, newResourceId);

                logger.info("Created new resource with ID " + newResourceId);

                return newResourceId;
            }
        }
    }

    private void cloneAndDisconnectTopic(int topicId) throws DatabaseException, SQLException {
        final class TopicConnection {
            private int id;
            private String type;
            private int parentId;
            private int topicId;
            private int rank;

            public TopicConnection(String type, int id, int parentId, int topicId, int rank) {
                this.type = type;
                this.id = id;
                this.parentId = parentId;
                this.topicId = topicId;
                this.rank = rank;
            }
        }

        try (var query = connection.prepareStatement("SELECT 'topic' as type, id, topic_id AS parent_id, subtopic_id AS topic_id, rank FROM topic_subtopic WHERE subtopic_id = " + topicId + " UNION SELECT 'subject' as type, id, subject_id AS parent_id, topic_id AS topic_id, rank FROM subject_topic WHERE topic_id = " + topicId)) {
            final var result = query.executeQuery();

            final var rows = new ArrayList<TopicConnection>();

            while (result.next()) {
                rows.add(new TopicConnection(result.getString("type"), result.getInt("id"), result.getInt("parent_id"), result.getInt("topic_id"), result.getInt("rank")));
            }

            if (rows.size() < 2) {
                throw new IllegalArgumentException("Could not get two or more parent topic/subject connections for topic with id " + topicId);
            }

            // Keep one connection
            rows.remove(0);

            for (final var connectionToClone : rows) {
                String tableToDeleteFrom;
                switch (connectionToClone.type) {
                    case "subject":
                        tableToDeleteFrom = "subject_topic";
                        break;
                    case "topic":
                        tableToDeleteFrom = "topic_subtopic";
                        break;
                    default:
                        throw new RuntimeException();
                }

                try (var deleteSubTopicsQuery = connection.prepareStatement("DELETE FROM " + tableToDeleteFrom + " WHERE id = " + connectionToClone.id)) {
                    if (deleteSubTopicsQuery.executeUpdate() != 1) {
                        throw new IllegalStateException("Got wrong deleted rows count from delete");
                    }
                }

                final var newTopicId = cloneTopic(topicId);

                switch (connectionToClone.type) {
                    case "topic": {
                        final var newPublicId = "urn:topic-subtopic:" + randomUUID();

                        logger.info("Connecting parent topic " + connectionToClone.parentId + " to subtopic " + newTopicId);
                        try (var createConnectionQuery = connection.prepareStatement("INSERT INTO topic_subtopic (public_id, topic_id, subtopic_id, rank, is_primary) VALUES(?, ?, ?, ?, true)")) {
                            createConnectionQuery.setString(1, newPublicId);
                            createConnectionQuery.setInt(2, connectionToClone.parentId);
                            createConnectionQuery.setInt(3, newTopicId);
                            createConnectionQuery.setInt(4, connectionToClone.rank);

                            if (createConnectionQuery.executeUpdate() != 1) {
                                throw new RuntimeException("Unable to create new parent topic connection");
                            }
                        }
                        break;
                    }
                    case "subject": {
                        final var newPublicId = "urn:subject-topic:" + randomUUID();

                        logger.info("Connecting parent subject " + connectionToClone.parentId + " to topic " + newTopicId);
                        try (var createConnectionQuery = connection.prepareStatement("INSERT INTO subject_topic (public_id, subject_id, topic_id, rank, is_primary) VALUES(?, ?, ?, ?, true)")) {
                            createConnectionQuery.setString(1, newPublicId);
                            createConnectionQuery.setInt(2, connectionToClone.parentId);
                            createConnectionQuery.setInt(3, newTopicId);
                            createConnectionQuery.setInt(4, connectionToClone.rank);

                            if (createConnectionQuery.executeUpdate() != 1) {
                                throw new RuntimeException("Unable to create new parent subject connection");
                            }
                        }
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("Unknown type");
                }
            }
        }
    }

    private void cloneAndDisconnectTopicResource(int resourceId) throws DatabaseException, SQLException {
        final class TopicResourceConnection {
            private int id;
            private int topicId;
            private int resourceId;
            private int rank;

            public TopicResourceConnection(int id, int topicId, int resourceId, int rank) {
                this.id = id;
                this.topicId = topicId;
                this.resourceId = resourceId;
                this.rank = rank;
            }
        }

        try (var query = connection.prepareStatement("SELECT id, topic_id, resource_id, rank FROM topic_resource WHERE resource_id = " + resourceId); var result = query.executeQuery()) {

            final var rows = new ArrayList<TopicResourceConnection>();

            while (result.next()) {
                rows.add(new TopicResourceConnection(result.getInt("id"), result.getInt("topic_id"), result.getInt("resource_id"), result.getInt("rank")));
            }

            if (rows.size() < 2) {
                throw new IllegalArgumentException("Could not get two or more parent topic connections for resource with id" + resourceId);
            }

            // Keep one connection
            rows.remove(0);

            for (final var connectionToClone : rows) {
                try (var topicResourceDeleteQuery = connection.prepareStatement("DELETE FROM topic_resource WHERE id = " + connectionToClone.id)) {
                    if (topicResourceDeleteQuery.executeUpdate() != 1) {
                        throw new IllegalStateException("Got wrong delete row count when deleting one connection");
                    }
                }

                final var newResourceId = cloneResource(resourceId);
                final var newPublicId = "urn:topic-resource:" + randomUUID();

                logger.info("Connecting parent topic " + connectionToClone.topicId + " to resource " + newResourceId);
                try (var createConnectionQuery = connection.prepareStatement("INSERT INTO topic_resource (public_id, topic_id, resource_id, rank, is_primary) VALUES(?, ?, ?, ?, true)")) {
                    createConnectionQuery.setString(1, newPublicId);
                    createConnectionQuery.setInt(2, connectionToClone.topicId);
                    createConnectionQuery.setInt(3, newResourceId);
                    createConnectionQuery.setInt(4, connectionToClone.rank);

                    if (createConnectionQuery.executeUpdate() != 1) {
                        throw new RuntimeException("Unable to create new parent topic connection for resource");
                    }
                }
            }
        }
    }

    private void verifyNoSharedEntitiesExists() throws DatabaseException, SQLException, CustomChangeException {
        try (var topicQuery = connection.prepareStatement("SELECT COUNT(parent), child FROM (SELECT topic_id as parent, subtopic_id as child FROM topic_subtopic UNION SELECT subject_id as parent, topic_id AS child FROM subject_topic) connections GROUP by child")) {
            final var result = topicQuery.executeQuery();
            while (result.next()) {
                if (result.getInt(1) > 1) {
                    throw new CustomChangeException("Topic with ID " + result.getInt(2) + " still has more than one parent connection");
                }
            }
        }

        try (var resourceQuery = connection.prepareStatement("SELECT COUNT(parent), child FROM (SELECT topic_id as parent, resource_id as child FROM topic_resource) connections GROUP by child")) {
            final var result = resourceQuery.executeQuery();
            while (result.next()) {
                if (result.getInt(1) > 1) {
                    throw new CustomChangeException("Resource with ID " + result.getInt(2) + " still has more than one parent connection");
                }
            }
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
            var countSharedTopics = 0;
            var countSharedResources = 0;
            var iterations = 0;
            do {
                logger.info("Starting iteration " + (iterations + 1));
                if (iterations++ > 10) {
                    throw new CustomChangeException("Too many iterations when cloning topics");
                }

                countSharedTopics = 0;
                countSharedResources = 0;

                try (var findSharedTopicSubtopicsQuery = connection.prepareStatement("SELECT child_id FROM (SELECT ts.topic_id AS parent_id, ts.subtopic_id AS child_id FROM topic_subtopic AS ts UNION SELECT st.subject_id AS parent_id, st.topic_id AS child_id FROM subject_topic AS st) AS connections GROUP BY child_id HAVING COUNT(parent_id) > 1")) {
                    final var sharedTopicsQueryResult = findSharedTopicSubtopicsQuery.executeQuery();

                    while (sharedTopicsQueryResult.next()) {
                        countSharedTopics++;

                        final var childId = sharedTopicsQueryResult.getInt("child_id");
                        cloneAndDisconnectTopic(childId);
                    }
                }

                try (var findSharedTopicResourcesQuery = connection.prepareStatement("SELECT resource_id FROM topic_resource GROUP BY resource_id HAVING COUNT(id) > 1")) {
                    final var sharedResourcesQueryResult = findSharedTopicResourcesQuery.executeQuery();

                    while (sharedResourcesQueryResult.next()) {
                        countSharedResources++;

                        cloneAndDisconnectTopicResource(sharedResourcesQueryResult.getInt("resource_id"));
                    }
                }

            } while (countSharedTopics > 0 || countSharedResources > 0);

            verifyNoSharedEntitiesExists();

            return new SqlStatement[0];
        } catch (Exception e) {
            try {
                connection.rollback();
                throw new CustomChangeException(e.getClass().toString() + " while doing migration. Transaction rolled back. Error: " + e.getMessage(), e);
            } catch (DatabaseException e2) {
                throw new CustomChangeException(e.getClass().toString() + " while doing migration. Error " + e.getMessage() + " Error rolling back transaction: " + e2.getMessage(), e2);
            }
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "Removed all shared topics and resources";
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
