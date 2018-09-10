package db.migration;

import no.ndla.taxonomy.service.OldUrlCanonifier;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 */
public class V21__UrlMapRefactoring implements JdbcMigration {

    OldUrlCanonifier canonifier = new OldUrlCanonifier();

    public void migrate(Connection connection) throws SQLException {
        ResultSet result = connection.prepareStatement("SELECT old_url from URL_MAP").executeQuery();
        if (result != null) {
            connection.prepareStatement("ALTER TABLE URL_MAP ADD COLUMN old_node_id VARCHAR(10);").execute();
            connection.prepareStatement("ALTER TABLE URL_MAP ADD COLUMN old_subject_id VARCHAR(10);").execute();

            while (result.next()) {
                String oldUrl = result.getString(1);
                String canonified =  canonifier.canonify(oldUrl);
                int paramIndex = canonified.indexOf("?");
                int nodeStart = canonified.indexOf("/node");
                int nodeIdStart = canonified.indexOf("/", (nodeStart+1))+1;
                String nodeId = paramIndex != -1 ? canonified.substring(nodeIdStart, paramIndex): canonified.substring(nodeIdStart);
                System.out.println("Setting node Id to "+nodeId);
                PreparedStatement update = connection.prepareStatement("update URL_MAP set old_node_id = ? where old_url = ?");
                update.setString(1, nodeId);
                update.setString(2, oldUrl);
                update.execute();
                PreparedStatement updateID = connection.prepareStatement("update URL_MAP set old_url = ? where old_url = ?");
                updateID.setString(1, canonified);
                updateID.setString(2, oldUrl);
                updateID.execute();

            }
        }
    }


}