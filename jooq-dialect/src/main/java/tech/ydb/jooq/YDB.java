package tech.ydb.jooq;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import org.jooq.Configuration;
import org.jooq.ConnectionProvider;
import org.jooq.Field;
import org.jooq.JSON;
import org.jooq.JSONB;
import org.jooq.Param;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.ConnectionUtils;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConnectionProvider;
import org.jooq.types.UByte;
import org.jooq.types.UInteger;
import org.jooq.types.ULong;
import org.jooq.types.UShort;
import tech.ydb.jooq.dsl.replace.*;
import tech.ydb.jooq.dsl.upsert.*;
import tech.ydb.jooq.impl.DefaultCloseableYdbDSLContext;
import tech.ydb.jooq.impl.YdbDSLContextImpl;
import tech.ydb.jooq.value.YSON;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Properties;
import java.util.UUID;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class YDB {

    public static final SQLDialect DIALECT = SQLDialect.DEFAULT;

    private YDB() {
        throw new UnsupportedOperationException();
    }

    /**
     * Create an executor.
     * <p>
     * Without a connection or data source, this executor cannot execute
     * queries. Use it to render SQL only.
     */
    public static YdbDSLContext using() {
        return new YdbDSLContextImpl();
    }

    /**
     * Create an executor with settings configured.
     * <p>
     * Without a connection or data source, this executor cannot execute
     * queries. Use it to render SQL only.
     *
     * @param settings The runtime settings to apply to objects created from
     *                 this executor
     */
    public static YdbDSLContext using(Settings settings) {
        return new YdbDSLContextImpl(settings);
    }

    /**
     * Create an executor from a JDBC connection URL.
     * <p>
     * Clients must ensure connections are closed properly by calling
     * {@link CloseableYdbDSLContext#close()} on the resulting {@link YdbDSLContext}.
     * For example:
     * <p>
     * <pre><code>
     * // Auto-closing YdbDSLContext instance to free resources
     * try (CloseableYdbDSLContext ctx = YDB.using("jdbc:ydb:grpc://localhost:2136/local")) {
     *
     *     // ...
     * }
     * </code></pre>
     * <p>
     * Both acquisition and release of JDBC connection URLs are blocking.
     *
     * @param url The connection URL.
     * @see DefaultConnectionProvider
     */
    public static CloseableYdbDSLContext using(String url) {
        try {
            Connection connection = DriverManager.getConnection(url);
            return new DefaultCloseableYdbDSLContext(ConnectionUtils.closeableProvider(connection));
        } catch (SQLException e) {
            throw initializeException(e);
        }
    }

    /**
     * Create an executor from a JDBC connection URL.
     * <p>
     * Clients must ensure connections are closed properly by calling
     * {@link CloseableYdbDSLContext#close()} on the resulting {@link YdbDSLContext}.
     * For example:
     * <p>
     * <pre><code>
     * // Auto-closing YdbDSLContext instance to free resources
     * try (CloseableYdbDSLContext ctx = YDB.using("jdbc:ydb:grpc://localhost:2136/local", "sa", "")) {
     *
     *     // ...
     * }
     * </code></pre>
     * <p>
     * Both acquisition and release of JDBC connection URLs are blocking.
     *
     * @param url      The connection URL.
     * @param username The connection username.
     * @param password The connection password.
     * @see DefaultConnectionProvider
     */
    public static CloseableYdbDSLContext using(String url, String username, String password) {
        try {
            Connection connection = DriverManager.getConnection(url, username, password);
            return new DefaultCloseableYdbDSLContext(ConnectionUtils.closeableProvider(connection));
        } catch (SQLException e) {
            throw initializeException(e);
        }
    }

    /**
     * Create an executor from a JDBC connection URL.
     * <p>
     * Clients must ensure connections are closed properly by calling
     * {@link CloseableYdbDSLContext#close()} on the resulting {@link YdbDSLContext}.
     * For example:
     * <p>
     * <pre><code>
     * // Auto-closing YdbDSLContext instance to free resources
     * try (CloseableYdbDSLContext ctx = YDB.using("jdbc:ydb:grpc://localhost:2136/local", properties)) {
     *
     *     // ...
     * }
     * </code></pre>
     * <p>
     * Both acquisition and release of JDBC connection URLs are blocking.
     *
     * @param url        The connection URL.
     * @param properties The connection properties.
     * @see DefaultConnectionProvider
     */
    public static CloseableYdbDSLContext using(String url, Properties properties) {
        try {
            Connection connection = DriverManager.getConnection(url, properties);
            return new DefaultCloseableYdbDSLContext(ConnectionUtils.closeableProvider(connection));
        } catch (SQLException e) {
            throw initializeException(e);
        }
    }

    /**
     * Create an executor with a connection configured.
     * <p>
     * If you provide a JDBC connection to a jOOQ Configuration, jOOQ will use
     * that connection directly for creating statements.
     * <p>
     * This is a convenience constructor for
     * {@link #using(Connection, Settings)}
     *
     * @param connection The connection to use with objects created from this
     *                   executor
     * @see DefaultConnectionProvider
     */
    public static YdbDSLContext using(Connection connection) {
        return new YdbDSLContextImpl(connection);
    }

    /**
     * Create an executor with a connection and settings configured.
     * <p>
     * If you provide a JDBC connection to a jOOQ Configuration, jOOQ will use
     * that connection directly for creating statements.
     * <p>
     * This is a convenience constructor for
     * {@link #using(ConnectionProvider, Settings)} using a
     * {@link DefaultConnectionProvider}
     *
     * @param connection The connection to use with objects created from this
     *                   executor
     * @param settings   The runtime settings to apply to objects created from
     *                   this executor
     * @see DefaultConnectionProvider
     */
    public static YdbDSLContext using(Connection connection, Settings settings) {
        return new YdbDSLContextImpl(connection, settings);
    }

    /**
     * Create an executor with a data source configured.
     * <p>
     * If you provide a JDBC data source to a jOOQ Configuration, jOOQ will use
     * that data source for initialising connections, and creating statements.
     * <p>
     * This is a convenience constructor for
     * {@link #using(ConnectionProvider)} using a
     * {@link DataSourceConnectionProvider}
     *
     * @param datasource The data source to use with objects created from this
     *                   executor
     * @see DataSourceConnectionProvider
     */
    public static YdbDSLContext using(DataSource datasource) {
        return new YdbDSLContextImpl(datasource);
    }

    /**
     * Create an executor with a data source and settings configured.
     * <p>
     * If you provide a JDBC data source to a jOOQ Configuration, jOOQ will use
     * that data source for initialising connections, and creating statements.
     * <p>
     * This is a convenience constructor for
     * {@link #using(ConnectionProvider, Settings)} using a
     * {@link DataSourceConnectionProvider}
     *
     * @param datasource The data source to use with objects created from this
     *                   executor
     * @param settings   The runtime settings to apply to objects created from
     *                   this executor
     * @see DataSourceConnectionProvider
     */
    public static YdbDSLContext using(DataSource datasource, Settings settings) {
        return new YdbDSLContextImpl(datasource, settings);
    }

    /**
     * Create an executor with a custom connection provider configured.
     *
     * @param connectionProvider The connection provider providing jOOQ with
     *                           JDBC connections
     */
    public static YdbDSLContext using(ConnectionProvider connectionProvider) {
        return new YdbDSLContextImpl(connectionProvider);
    }

    /**
     * Create an executor with a custom connection provider and settings configured.
     *
     * @param connectionProvider The connection provider providing jOOQ with
     *                           JDBC connections
     * @param settings           The runtime settings to apply to objects created from
     *                           this executor
     */
    public static YdbDSLContext using(ConnectionProvider connectionProvider, Settings settings) {
        return new YdbDSLContextImpl(connectionProvider, settings);
    }

    /**
     * Create an executor from a custom configuration.
     *
     * @param configuration The configuration
     */
    public static YdbDSLContext using(Configuration configuration) {
        return new YdbDSLContextImpl(configuration);
    }

    private static RuntimeException initializeException(Exception e) {
        return new DataAccessException("SQL [Error when initialising Connection]; " + e.getMessage(), e);
    }

    private static YdbDSLContext dsl() {
        return using();
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table)
     *   .set(field1, value1)
     *   .set(field2, value2)
     *   .newRecord()
     *   .set(field1, value3)
     *   .set(field2, value4)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table)
     */
    public static <R extends Record> UpsertSetStep<R> upsertInto(Table<R> into) {
        return dsl().upsertInto(into);
    }


    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1)
     *   .values(field1)
     *   .values(field1)
     *   .onDuplicateKeyUpdate()
     *   .set(field1, value1)
     *   .set(field2, value2)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field)
     */
    public static <R extends Record, T1> UpsertValuesStep1<R, T1> upsertInto(Table<R> into, Field<T1> field1) {
        return (UpsertValuesStep1) upsertInto(into, new Field[]{field1});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2)
     *   .values(field1, field2)
     *   .values(field1, field2)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field)
     */
    public static <R extends Record, T1, T2> UpsertValuesStep2<R, T1, T2> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2) {
        return (UpsertValuesStep2) upsertInto(into, new Field[]{field1, field2});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3)
     *   .values(field1, field2, field3)
     *   .values(field1, field2, field3)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3> UpsertValuesStep3<R, T1, T2, T3> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3) {
        return (UpsertValuesStep3) upsertInto(into, new Field[]{field1, field2, field3});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, field4)
     *   .values(field1, field2, field3, field4)
     *   .values(field1, field2, field3, field4)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4> UpsertValuesStep4<R, T1, T2, T3, T4> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4) {
        return (UpsertValuesStep4) upsertInto(into, new Field[]{field1, field2, field3, field4});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, field4, field5)
     *   .values(field1, field2, field3, field4, field5)
     *   .values(field1, field2, field3, field4, field5)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5> UpsertValuesStep5<R, T1, T2, T3, T4, T5> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5) {
        return (UpsertValuesStep5) upsertInto(into, new Field[]{field1, field2, field3, field4, field5});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field5, field6)
     *   .values(valueA1, valueA2, valueA3, .., valueA5, valueA6)
     *   .values(valueB1, valueB2, valueB3, .., valueB5, valueB6)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6> UpsertValuesStep6<R, T1, T2, T3, T4, T5, T6> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6) {
        return (UpsertValuesStep6) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field6, field7)
     *   .values(valueA1, valueA2, valueA3, .., valueA6, valueA7)
     *   .values(valueB1, valueB2, valueB3, .., valueB6, valueB7)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7> UpsertValuesStep7<R, T1, T2, T3, T4, T5, T6, T7> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7) {
        return (UpsertValuesStep7) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field7, field8)
     *   .values(valueA1, valueA2, valueA3, .., valueA7, valueA8)
     *   .values(valueB1, valueB2, valueB3, .., valueB7, valueB8)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8> UpsertValuesStep8<R, T1, T2, T3, T4, T5, T6, T7, T8> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8) {
        return (UpsertValuesStep8) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field8, field9)
     *   .values(valueA1, valueA2, valueA3, .., valueA8, valueA9)
     *   .values(valueB1, valueB2, valueB3, .., valueB8, valueB9)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9> UpsertValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9) {
        return (UpsertValuesStep9) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field9, field10)
     *   .values(valueA1, valueA2, valueA3, .., valueA9, valueA10)
     *   .values(valueB1, valueB2, valueB3, .., valueB9, valueB10)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> UpsertValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10) {
        return (UpsertValuesStep10) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field10, field11)
     *   .values(valueA1, valueA2, valueA3, .., valueA10, valueA11)
     *   .values(valueB1, valueB2, valueB3, .., valueB10, valueB11)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> UpsertValuesStep11<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11) {
        return (UpsertValuesStep11) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field11, field12)
     *   .values(valueA1, valueA2, valueA3, .., valueA11, valueA12)
     *   .values(valueB1, valueB2, valueB3, .., valueB11, valueB12)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> UpsertValuesStep12<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12) {
        return (UpsertValuesStep12) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field12, field13)
     *   .values(valueA1, valueA2, valueA3, .., valueA12, valueA13)
     *   .values(valueB1, valueB2, valueB3, .., valueB12, valueB13)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> UpsertValuesStep13<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13) {
        return (UpsertValuesStep13) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field13, field14)
     *   .values(valueA1, valueA2, valueA3, .., valueA13, valueA14)
     *   .values(valueB1, valueB2, valueB3, .., valueB13, valueB14)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> UpsertValuesStep14<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14) {
        return (UpsertValuesStep14) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field14, field15)
     *   .values(valueA1, valueA2, valueA3, .., valueA14, valueA15)
     *   .values(valueB1, valueB2, valueB3, .., valueB14, valueB15)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> UpsertValuesStep15<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15) {
        return (UpsertValuesStep15) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field15, field16)
     *   .values(valueA1, valueA2, valueA3, .., valueA15, valueA16)
     *   .values(valueB1, valueB2, valueB3, .., valueB15, valueB16)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> UpsertValuesStep16<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16) {
        return (UpsertValuesStep16) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field16, field17)
     *   .values(valueA1, valueA2, valueA3, .., valueA16, valueA17)
     *   .values(valueB1, valueB2, valueB3, .., valueB16, valueB17)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> UpsertValuesStep17<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17) {
        return (UpsertValuesStep17) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field17, field18)
     *   .values(valueA1, valueA2, valueA3, .., valueA17, valueA18)
     *   .values(valueB1, valueB2, valueB3, .., valueB17, valueB18)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> UpsertValuesStep18<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18) {
        return (UpsertValuesStep18) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field18, field19)
     *   .values(valueA1, valueA2, valueA3, .., valueA18, valueA19)
     *   .values(valueB1, valueB2, valueB3, .., valueB18, valueB19)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> UpsertValuesStep19<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19) {
        return (UpsertValuesStep19) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field19, field20)
     *   .values(valueA1, valueA2, valueA3, .., valueA19, valueA20)
     *   .values(valueB1, valueB2, valueB3, .., valueB19, valueB20)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> UpsertValuesStep20<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20) {
        return (UpsertValuesStep20) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field20, field21)
     *   .values(valueA1, valueA2, valueA3, .., valueA20, valueA21)
     *   .values(valueB1, valueB2, valueB3, .., valueB20, valueB21)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> UpsertValuesStep21<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20, Field<T21> field21) {
        return (UpsertValuesStep21) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20, field21});
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2, field3, .., field21, field22)
     *   .values(valueA1, valueA2, valueA3, .., valueA21, valueA22)
     *   .values(valueB1, valueB2, valueB3, .., valueB21, valueB22)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> UpsertValuesStep22<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20, Field<T21> field21, Field<T22> field22) {
        return (UpsertValuesStep22) upsertInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20, field21, field22});
    }


    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2)
     *   .values(valueA1, valueA2)
     *   .values(valueB1, valueB2)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Field...)
     */
    public static <R extends Record> UpsertValuesStepN<R> upsertInto(Table<R> into, Field<?>... fields) {
        return dsl().upsertInto(into, fields);
    }

    /**
     * Create a new DSL upsert statement.
     * <p>
     * Unlike {@link Upsert} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>UPSERT</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * upsertInto(table, field1, field2)
     *   .values(valueA1, valueA2)
     *   .values(valueB1, valueB2)
     * </code></pre>
     *
     * @see YdbDSLContext#upsertInto(Table, Collection)
     */
    public static <R extends Record> UpsertValuesStepN<R> upsertInto(Table<R> into, Collection<? extends Field<?>> fields) {
        return dsl().upsertInto(into, fields);
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table)
     *   .set(field1, value1)
     *   .set(field2, value2)
     *   .newRecord()
     *   .set(field1, value3)
     *   .set(field2, value4)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table)
     */
    public static <R extends Record> ReplaceSetStep<R> replaceInto(Table<R> into) {
        return dsl().replaceInto(into);
    }


    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1)
     *   .values(field1)
     *   .values(field1)
     *   .onDuplicateKeyUpdate()
     *   .set(field1, value1)
     *   .set(field2, value2)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field)
     */
    public static <R extends Record, T1> ReplaceValuesStep1<R, T1> replaceInto(Table<R> into, Field<T1> field1) {
        return (ReplaceValuesStep1) replaceInto(into, new Field[]{field1});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2)
     *   .values(field1, field2)
     *   .values(field1, field2)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field)
     */
    public static <R extends Record, T1, T2> ReplaceValuesStep2<R, T1, T2> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2) {
        return (ReplaceValuesStep2) replaceInto(into, new Field[]{field1, field2});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3)
     *   .values(field1, field2, field3)
     *   .values(field1, field2, field3)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3> ReplaceValuesStep3<R, T1, T2, T3> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3) {
        return (ReplaceValuesStep3) replaceInto(into, new Field[]{field1, field2, field3});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, field4)
     *   .values(field1, field2, field3, field4)
     *   .values(field1, field2, field3, field4)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4> ReplaceValuesStep4<R, T1, T2, T3, T4> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4) {
        return (ReplaceValuesStep4) replaceInto(into, new Field[]{field1, field2, field3, field4});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, field4, field5)
     *   .values(field1, field2, field3, field4, field5)
     *   .values(field1, field2, field3, field4, field5)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5> ReplaceValuesStep5<R, T1, T2, T3, T4, T5> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5) {
        return (ReplaceValuesStep5) replaceInto(into, new Field[]{field1, field2, field3, field4, field5});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field5, field6)
     *   .values(valueA1, valueA2, valueA3, .., valueA5, valueA6)
     *   .values(valueB1, valueB2, valueB3, .., valueB5, valueB6)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6> ReplaceValuesStep6<R, T1, T2, T3, T4, T5, T6> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6) {
        return (ReplaceValuesStep6) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field6, field7)
     *   .values(valueA1, valueA2, valueA3, .., valueA6, valueA7)
     *   .values(valueB1, valueB2, valueB3, .., valueB6, valueB7)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7> ReplaceValuesStep7<R, T1, T2, T3, T4, T5, T6, T7> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7) {
        return (ReplaceValuesStep7) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field7, field8)
     *   .values(valueA1, valueA2, valueA3, .., valueA7, valueA8)
     *   .values(valueB1, valueB2, valueB3, .., valueB7, valueB8)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8> ReplaceValuesStep8<R, T1, T2, T3, T4, T5, T6, T7, T8> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8) {
        return (ReplaceValuesStep8) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field8, field9)
     *   .values(valueA1, valueA2, valueA3, .., valueA8, valueA9)
     *   .values(valueB1, valueB2, valueB3, .., valueB8, valueB9)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9> ReplaceValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9) {
        return (ReplaceValuesStep9) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field9, field10)
     *   .values(valueA1, valueA2, valueA3, .., valueA9, valueA10)
     *   .values(valueB1, valueB2, valueB3, .., valueB9, valueB10)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> ReplaceValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10) {
        return (ReplaceValuesStep10) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field10, field11)
     *   .values(valueA1, valueA2, valueA3, .., valueA10, valueA11)
     *   .values(valueB1, valueB2, valueB3, .., valueB10, valueB11)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> ReplaceValuesStep11<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11) {
        return (ReplaceValuesStep11) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field11, field12)
     *   .values(valueA1, valueA2, valueA3, .., valueA11, valueA12)
     *   .values(valueB1, valueB2, valueB3, .., valueB11, valueB12)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> ReplaceValuesStep12<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12) {
        return (ReplaceValuesStep12) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field12, field13)
     *   .values(valueA1, valueA2, valueA3, .., valueA12, valueA13)
     *   .values(valueB1, valueB2, valueB3, .., valueB12, valueB13)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> ReplaceValuesStep13<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13) {
        return (ReplaceValuesStep13) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field13, field14)
     *   .values(valueA1, valueA2, valueA3, .., valueA13, valueA14)
     *   .values(valueB1, valueB2, valueB3, .., valueB13, valueB14)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> ReplaceValuesStep14<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14) {
        return (ReplaceValuesStep14) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field14, field15)
     *   .values(valueA1, valueA2, valueA3, .., valueA14, valueA15)
     *   .values(valueB1, valueB2, valueB3, .., valueB14, valueB15)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> ReplaceValuesStep15<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15) {
        return (ReplaceValuesStep15) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field15, field16)
     *   .values(valueA1, valueA2, valueA3, .., valueA15, valueA16)
     *   .values(valueB1, valueB2, valueB3, .., valueB15, valueB16)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> ReplaceValuesStep16<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16) {
        return (ReplaceValuesStep16) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field16, field17)
     *   .values(valueA1, valueA2, valueA3, .., valueA16, valueA17)
     *   .values(valueB1, valueB2, valueB3, .., valueB16, valueB17)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> ReplaceValuesStep17<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17) {
        return (ReplaceValuesStep17) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field17, field18)
     *   .values(valueA1, valueA2, valueA3, .., valueA17, valueA18)
     *   .values(valueB1, valueB2, valueB3, .., valueB17, valueB18)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> ReplaceValuesStep18<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18) {
        return (ReplaceValuesStep18) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field18, field19)
     *   .values(valueA1, valueA2, valueA3, .., valueA18, valueA19)
     *   .values(valueB1, valueB2, valueB3, .., valueB18, valueB19)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> ReplaceValuesStep19<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19) {
        return (ReplaceValuesStep19) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field19, field20)
     *   .values(valueA1, valueA2, valueA3, .., valueA19, valueA20)
     *   .values(valueB1, valueB2, valueB3, .., valueB19, valueB20)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> ReplaceValuesStep20<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20) {
        return (ReplaceValuesStep20) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field20, field21)
     *   .values(valueA1, valueA2, valueA3, .., valueA20, valueA21)
     *   .values(valueB1, valueB2, valueB3, .., valueB20, valueB21)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> ReplaceValuesStep21<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20, Field<T21> field21) {
        return (ReplaceValuesStep21) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20, field21});
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2, field3, .., field21, field22)
     *   .values(valueA1, valueA2, valueA3, .., valueA21, valueA22)
     *   .values(valueB1, valueB2, valueB3, .., valueB21, valueB22)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)
     */
    public static <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> ReplaceValuesStep22<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20, Field<T21> field21, Field<T22> field22) {
        return (ReplaceValuesStep22) replaceInto(into, new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20, field21, field22});
    }


    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2)
     *   .values(valueA1, valueA2)
     *   .values(valueB1, valueB2)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Field...)
     */
    public static <R extends Record> ReplaceValuesStepN<R> replaceInto(Table<R> into, Field<?>... fields) {
        return dsl().replaceInto(into, fields);
    }

    /**
     * Create a new DSL replace statement.
     * <p>
     * Unlike {@link Replace} factory methods in the {@link YdbDSLContext} API, this
     * creates an unattached, and thus not directly renderable or executable
     * <code>REPLACE</code> statement.
     * <p>
     * Example: <pre><code>
     * import static org.jooq.impl.DSL.*;
     *
     * // [...]
     *
     * replaceInto(table, field1, field2)
     *   .values(valueA1, valueA2)
     *   .values(valueB1, valueB2)
     * </code></pre>
     *
     * @see YdbDSLContext#replaceInto(Table, Collection)
     */
    public static <R extends Record> ReplaceValuesStepN<R> replaceInto(Table<R> into, Collection<? extends Field<?>> fields) {
        return dsl().replaceInto(into, fields);
    }

    /**
     * Get a bind value.
     */
    public static <T> Param<T> val(T value) {
        if (value instanceof Byte b) {
            return (Param<T>) val(b);
        } else if (value instanceof Short s) {
            return (Param<T>) val(s);
        } else if (value instanceof Integer i) {
            return (Param<T>) val(i);
        } else if (value instanceof Long l) {
            return (Param<T>) val(l);
        } else if (value instanceof UByte b) {
            return (Param<T>) val(b);
        } else if (value instanceof UShort s) {
            return (Param<T>) val(s);
        } else if (value instanceof UInteger i) {
            return (Param<T>) val(i);
        } else if (value instanceof ULong l) {
            return (Param<T>) val(l);
        } else if (value instanceof Float f) {
            return (Param<T>) val(f);
        } else if (value instanceof Double d) {
            return (Param<T>) val(d);
        } else if (value instanceof Boolean b) {
            return (Param<T>) val(b);
        } else if (value instanceof BigDecimal d) {
            return (Param<T>) val(d);
        } else if (value instanceof byte[] b) {
            return (Param<T>) val(b);
        } else if (value instanceof String s) {
            return (Param<T>) val(s);
        } else if (value instanceof JSON j) {
            return (Param<T>) val(j);
        } else if (value instanceof JSONB j) {
            return (Param<T>) val(j);
        } else if (value instanceof YSON y) {
            return (Param<T>) val(y);
        } else if (value instanceof UUID u) {
            return (Param<T>) val(u);
        } else if (value instanceof LocalDate l) {
            return (Param<T>) val(l);
        } else if (value instanceof LocalDateTime l) {
            return (Param<T>) val(l);
        } else if (value instanceof Instant i) {
            return (Param<T>) val(i);
        } else if (value instanceof Duration d) {
            return (Param<T>) val(d);
        } else if (value instanceof ZonedDateTime z) {
            return (Param<T>) val(z);
        }
        throw new UnsupportedOperationException((value != null ? value.getClass().toString() : "[null]") + " class is not supported");
    }

    /**
     * Get a bind value.
     */
    public static Param<Byte> val(byte value) {
        return DSL.val(value, YdbTypes.INT8);
    }

    /**
     * Get a bind value.
     */
    public static Param<Byte> val(Byte value) {
        return DSL.val(value, YdbTypes.INT8);
    }

    /**
     * Get a bind value.
     */
    public static Param<UByte> val(UByte value) {
        return DSL.val(value, YdbTypes.UINT8);
    }

    /**
     * Get a bind value.
     */
    public static Param<Short> val(short value) {
        return DSL.val(value, YdbTypes.INT16);
    }

    /**
     * Get a bind value.
     */
    public static Param<Short> val(Short value) {
        return DSL.val(value, YdbTypes.INT16);
    }

    /**
     * Get a bind value.
     */
    public static Param<UShort> val(UShort value) {
        return DSL.val(value, YdbTypes.UINT16);
    }

    /**
     * Get a bind value.
     */
    public static Param<Integer> val(int value) {
        return DSL.val(value, YdbTypes.INT32);
    }

    /**
     * Get a bind value.
     */
    public static Param<Integer> val(Integer value) {
        return DSL.val(value, YdbTypes.INT32);
    }

    /**
     * Get a bind value.
     */
    public static Param<UInteger> val(UInteger value) {
        return DSL.val(value, YdbTypes.UINT32);
    }

    /**
     * Get a bind value.
     */
    public static Param<Long> val(long value) {
        return DSL.val(value, YdbTypes.INT64);
    }

    /**
     * Get a bind value.
     */
    public static Param<Long> val(Long value) {
        return DSL.val(value, YdbTypes.INT64);
    }

    /**
     * Get a bind value.
     */
    public static Param<ULong> val(ULong value) {
        return DSL.val(value, YdbTypes.UINT64);
    }

    /**
     * Get a bind value.
     */
    public static Param<Float> val(float value) {
        return DSL.val(value, YdbTypes.FLOAT);
    }

    /**
     * Get a bind value.
     */
    public static Param<Float> val(Float value) {
        return DSL.val(value, YdbTypes.FLOAT);
    }

    /**
     * Get a bind value.
     */
    public static Param<Double> val(double value) {
        return DSL.val(value, YdbTypes.DOUBLE);
    }

    /**
     * Get a bind value.
     */
    public static Param<Double> val(Double value) {
        return DSL.val(value, YdbTypes.DOUBLE);
    }

    /**
     * Get a bind value.
     */
    public static Param<Boolean> val(boolean value) {
        return DSL.val(value, YdbTypes.BOOL);
    }

    /**
     * Get a bind value.
     */
    public static Param<Boolean> val(Boolean value) {
        return DSL.val(value, YdbTypes.BOOL);
    }

    /**
     * Get a bind value.
     */
    public static Param<BigDecimal> val(BigDecimal value) {
        return DSL.val(value, YdbTypes.DECIMAL);
    }

    /**
     * Get a bind value.
     */
    public static Param<byte[]> val(byte[] value) {
        return DSL.val(value, YdbTypes.STRING);
    }

    /**
     * Get a bind value.
     */
    public static Param<String> val(String value) {
        return DSL.val(value, YdbTypes.UTF8);
    }

    /**
     * Get a bind value.
     */
    public static Param<JSON> val(JSON value) {
        return DSL.val(value, YdbTypes.JSON);
    }

    /**
     * Get a bind value.
     */
    public static Param<JSONB> val(JSONB value) {
        return DSL.val(value, YdbTypes.JSONDOCUMENT);
    }

    /**
     * Get a bind value.
     */
    public static Param<YSON> val(YSON value) {
        return DSL.val(value, YdbTypes.YSON);
    }

    /**
     * Get a bind value.
     */
    public static Param<UUID> val(UUID value) {
        return DSL.val(value, YdbTypes.UUID);
    }

    /**
     * Get a bind value.
     */
    public static Param<LocalDate> val(LocalDate value) {
        return DSL.val(value, YdbTypes.DATE);
    }

    /**
     * Get a bind value.
     */
    public static Param<LocalDateTime> val(LocalDateTime value) {
        return DSL.val(value, YdbTypes.DATETIME);
    }

    /**
     * Get a bind value.
     */
    public static Param<Instant> val(Instant value) {
        return DSL.val(value, YdbTypes.TIMESTAMP);
    }

    /**
     * Get a bind value.
     */
    public static Param<Duration> val(Duration value) {
        return DSL.val(value, YdbTypes.INTERVAL);
    }

    /**
     * Get a bind value.
     */
    public static Param<ZonedDateTime> val(ZonedDateTime value) {
        return DSL.val(value, YdbTypes.TZ_DATETIME);
    }

    /**
     * Get a bind value.
     */
    public static Param<ZonedDateTime> tzDate(ZonedDateTime value) {
        return DSL.val(value, YdbTypes.TZ_DATE);
    }

    /**
     * Get a bind value.
     */
    public static Param<ZonedDateTime> tzDateTime(ZonedDateTime value) {
        return DSL.val(value, YdbTypes.TZ_DATETIME);
    }

    /**
     * Get a bind value.
     */
    public static Param<ZonedDateTime> tzTimestamp(ZonedDateTime value) {
        return DSL.val(value, YdbTypes.TZ_TIMESTAMP);
    }
}
