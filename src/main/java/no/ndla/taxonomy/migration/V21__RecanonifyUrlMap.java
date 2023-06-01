/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.migration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.RawSqlStatement;
import no.ndla.taxonomy.service.OldUrlCanonifier;

public class V21__RecanonifyUrlMap implements CustomSqlChange {

    @Override
    public String getConfirmationMessage() {
        return "V21__RecanonifyUrlMap updated";
    }

    @Override
    public void setUp() {}

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {}

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }

    @Override
    public SqlStatement[] generateStatements(Database database) {
        List<SqlStatement> statementsToReturn = new ArrayList<>();

        OldUrlCanonifier canonifier = new OldUrlCanonifier();

        JdbcConnection connection = (JdbcConnection) database.getConnection();

        try {
            ResultSet result =
                    connection.prepareStatement("SELECT old_url from URL_MAP").executeQuery();
            if (result != null) {
                while (result.next()) {
                    String oldUrl = result.getString(1);
                    String canonified = canonifier.canonify(oldUrl);

                    String updateQuery = "update URL_MAP" + "               set old_url = "
                            + database.escapeStringForDatabase(canonified) + "               where old_url = "
                            + database.escapeStringForDatabase(oldUrl);

                    statementsToReturn.add(new RawSqlStatement(updateQuery));
                }
            }
        } catch (SQLException | DatabaseException exception) {
            // Should just fail the migration. No updates are run before returning from this method,
            // so no damage is done
            throw new RuntimeException(exception);
        }

        return statementsToReturn.toArray(new SqlStatement[0]);
    }
}
