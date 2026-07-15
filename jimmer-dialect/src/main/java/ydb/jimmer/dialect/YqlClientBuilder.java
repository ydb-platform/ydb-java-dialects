package ydb.jimmer.dialect;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.runtime.DefaultExecutor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import ydb.jimmer.dialect.scalar.DurationProvider;
import ydb.jimmer.dialect.transaction.YqlClient;
import ydb.jimmer.dialect.transaction.YdbTxConnectionManager;

import javax.sql.DataSource;
import java.util.function.Function;

public final class YqlClientBuilder {
    private YqlClientBuilder() {}

    public static YqlClient getYqlClient(
            DataSource dataSource,
            Function<JSqlClient.Builder, JSqlClient.Builder> block
    ) {
        return new YqlClient((JSqlClientImplementor) buildSqlClient(dataSource, block).build());
    }

    private static JSqlClient.Builder buildSqlClient(
            DataSource dataSource,
            Function<JSqlClient.Builder, JSqlClient.Builder> block
    ) {
        return block.apply(addScalarProviders(
                JSqlClient.newBuilder()
                        .setDialect(new YdbDialect())
                        .setConnectionManager(new YdbTxConnectionManager(dataSource))
                        .setCacheOperator(new UuidTransactionCacheOperator())
                        .setExecutor(new YdbExecutor(DefaultExecutor.INSTANCE))
        ));
    }

    public static YqlClient getYqlClient(DataSource dataSource) {
        return getYqlClient(dataSource, x -> x);
    }

    public static JSqlClient.Builder addScalarProviders(JSqlClient.Builder builder) {
        return builder.addScalarProvider(new DurationProvider());
    }
}
