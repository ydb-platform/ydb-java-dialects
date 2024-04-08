package tech.ydb.flywaydb.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.flywaydb.core.internal.database.DatabaseType;
import static org.flywaydb.core.internal.jdbc.JdbcNullTypes.BooleanNull;
import static org.flywaydb.core.internal.jdbc.JdbcNullTypes.IntegerNull;
import static org.flywaydb.core.internal.jdbc.JdbcNullTypes.StringNull;
import org.flywaydb.core.internal.jdbc.JdbcTemplate;

/**
 * @author Kirill Kurdyukov
 */
public class YdbJdbcTemplate extends JdbcTemplate  {

    public YdbJdbcTemplate(Connection connection, DatabaseType databaseType) {
        super(connection, databaseType);
    }

    @Override
    protected PreparedStatement prepareStatement(String sql, Object[] params) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            if (params[i] == StringNull) {
                statement.setNull(i + 1, Types.VARCHAR);
            } else if (params[i] == IntegerNull) {
                statement.setNull(i + 1, Types.INTEGER);
            } else if (params[i] == BooleanNull) {
                statement.setNull(i + 1, Types.BOOLEAN);
            } else {
                statement.setObject(i + 1, params[i]);
            }
        }

        return statement;
    }
}
