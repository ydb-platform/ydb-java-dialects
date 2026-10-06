package tech.ydb.trino;

import com.google.inject.Binder;
import com.google.inject.Scopes;
import io.trino.plugin.jdbc.ForBaseJdbc;
import io.trino.plugin.jdbc.JdbcClient;

public class TestingYdbJdbcModule extends YdbClientModule {

    @Override
    protected void bindJdbcClient(Binder binder) {
        binder.bind(JdbcClient.class).annotatedWith(ForBaseJdbc.class).to(TestingYdbJdbcClient.class).in(Scopes.SINGLETON);
    }
}
