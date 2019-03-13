package db.migration;

import no.ndla.taxonomy.services.OldUrlCanonifier;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class V21__RecanonifyUrlMap implements JdbcMigration {

    public void migrate(Connection connection) throws SQLException {
        OldUrlCanonifier canonifier = new OldUrlCanonifier();
        ResultSet result = connection.prepareStatement("SELECT old_url from URL_MAP").executeQuery();
        if (result != null) {
            while (result.next()) {
                String oldUrl = result.getString(1);
                String canonified = canonifier.canonify(oldUrl);
                PreparedStatement updateUrlStatement = connection.prepareStatement("update URL_MAP set old_url = ? where old_url = ?");
                updateUrlStatement.setString(1, canonified);
                updateUrlStatement.setString(2, oldUrl);
                updateUrlStatement.execute();
            }
        }
    }

}