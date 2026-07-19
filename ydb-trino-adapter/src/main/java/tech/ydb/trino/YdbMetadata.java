package tech.ydb.trino;

import io.trino.plugin.jdbc.DefaultJdbcMetadata;
import io.trino.plugin.jdbc.JdbcClient;
import io.trino.plugin.jdbc.JdbcQueryEventListener;
import io.trino.plugin.jdbc.TimestampTimeZoneDomain;

import java.util.Set;

public class YdbMetadata extends DefaultJdbcMetadata {
    public YdbMetadata(
            JdbcClient jdbcClient,
            TimestampTimeZoneDomain timestampTimeZoneDomain,
            Set<JdbcQueryEventListener> jdbcQueryEventListeners
    ) {
        super(jdbcClient, timestampTimeZoneDomain, false, jdbcQueryEventListeners);
    }
}
