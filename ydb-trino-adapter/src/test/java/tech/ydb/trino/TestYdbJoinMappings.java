package tech.ydb.trino;

import io.trino.plugin.base.mapping.DefaultIdentifierMapping;
import io.trino.plugin.jdbc.BaseJdbcConfig;
import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.plugin.jdbc.JdbcJoinCondition;
import io.trino.plugin.jdbc.JdbcJoinPushdownConfig;
import io.trino.plugin.jdbc.JdbcJoinPushdownSessionProperties;
import io.trino.plugin.jdbc.JdbcSortItem;
import io.trino.plugin.jdbc.JdbcTypeHandle;
import io.trino.plugin.jdbc.PreparedQuery;
import io.trino.plugin.jdbc.QueryParameter;
import io.trino.plugin.jdbc.logging.RemoteQueryModifier;
import io.trino.spi.connector.BasicRelationStatistics;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.JoinStatistics;
import io.trino.spi.connector.JoinType;
import io.trino.spi.connector.SortOrder;
import io.trino.spi.type.ArrayType;
import io.trino.spi.type.Type;
import io.trino.spi.type.TypeOperators;
import io.trino.testing.TestingConnectorSession;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.trino.spi.connector.JoinCondition.Operator.EQUAL;
import static io.trino.spi.connector.JoinCondition.Operator.IDENTICAL;
import static io.trino.spi.connector.JoinCondition.Operator.LESS_THAN;
import static io.trino.spi.function.InvocationConvention.InvocationArgumentConvention.BLOCK_POSITION;
import static io.trino.spi.function.InvocationConvention.InvocationReturnConvention.FAIL_ON_NULL;
import static io.trino.spi.function.InvocationConvention.simpleConvention;
import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.DecimalType.createDecimalType;
import static io.trino.spi.type.DoubleType.DOUBLE;
import static io.trino.spi.type.RealType.REAL;
import static io.trino.spi.type.TimestampType.createTimestampType;
import static io.trino.testing.TestingConnectorSession.SESSION;
import static org.assertj.core.api.Assertions.assertThat;

public class TestYdbJoinMappings {
    private final YdbClient client = new YdbClient(
            new BaseJdbcConfig(),
            _ -> {
                throw new SQLException("This test must not open a connection");
            },
            new YdbQueryBuilder(RemoteQueryModifier.NONE),
            new DefaultIdentifierMapping(),
            RemoteQueryModifier.NONE);

    @Test
    public void testSupportedNativeEqualityKeys() {
        for (String name : List.of("Bool", "Int8", "Int16", "Int32", "Int64", "Uint8", "Uint16", "Uint32", "Uint64",
                "Float", "Double", "Utf8", "String", "Date", "Date32", "Datetime", "Datetime64", "Timestamp", "Timestamp64")) {
            JdbcTypeHandle handle = typeHandle(name);
            Type type = client.toColumnMapping(SESSION, null, handle).orElseThrow().getType();
            JdbcColumnHandle left = new JdbcColumnHandle("left", handle, type);
            JdbcColumnHandle right = new JdbcColumnHandle("right", handle, type);
            assertThat(client.isSupportedJoinCondition(SESSION, new JdbcJoinCondition(left, EQUAL, right))).as(name).isTrue();
            assertThat(client.isSupportedJoinCondition(SESSION, new JdbcJoinCondition(left, LESS_THAN, right))).as(name).isFalse();
            assertThat(client.isSupportedJoinCondition(SESSION, new JdbcJoinCondition(left, IDENTICAL, right))).as(name).isFalse();
        }
    }

    @Test
    public void testDecimalAndMixedTypesFallBack() {
        JdbcColumnHandle decimal = new JdbcColumnHandle("decimal", typeHandle("Decimal(20,0)"), createDecimalType(20, 0));
        JdbcColumnHandle unsigned = new JdbcColumnHandle("unsigned", typeHandle("Uint64"), createDecimalType(20, 0));
        JdbcColumnHandle signed = new JdbcColumnHandle("signed", typeHandle("Int64"), BIGINT);
        assertThat(client.isSupportedJoinCondition(SESSION, new JdbcJoinCondition(decimal, EQUAL, decimal))).isFalse();
        assertThat(client.isSupportedJoinCondition(SESSION, new JdbcJoinCondition(unsigned, EQUAL, decimal))).isFalse();
        assertThat(client.isSupportedJoinCondition(SESSION, new JdbcJoinCondition(unsigned, EQUAL, signed))).isFalse();
    }

    @Test
    public void testFloatingPointNormalizationAndTypedTopN() throws Throwable {
        YdbQueryBuilder queryBuilder = new YdbQueryBuilder(RemoteQueryModifier.NONE);
        for (Type type : List.of(REAL, DOUBLE)) {
            String nativeType = type.equals(REAL) ? "Float" : "Double";
            JdbcColumnHandle column = new JdbcColumnHandle("key", typeHandle(nativeType), type);
            String condition = queryBuilder.formatJoinCondition(client, "l", "r", new JdbcJoinCondition(column, EQUAL, column));
            assertThat(condition).contains("NANVL(", "CAST(NULL AS " + nativeType + ")", "l.`key`", "r.`key`");
            var values = type.createBlockBuilder(null, 2);
            if (type.equals(REAL)) {
                type.writeLong(values, Float.floatToRawIntBits(Float.NaN));
                type.writeLong(values, Float.floatToRawIntBits(1.0f));
            } else {
                type.writeDouble(values, Double.NaN);
                type.writeDouble(values, 1.0);
            }
            var block = values.build();
            for (SortOrder order : SortOrder.values()) {
                int comparison = (int) new TypeOperators().getOrderingOperator(type, order,
                        simpleConvention(FAIL_ON_NULL, BLOCK_POSITION, BLOCK_POSITION)).invokeWithArguments(block, 0, block, 1);
                String nanDirection = comparison < 0 ? "DESC" : "ASC";
                String topN = client.topNFunction().orElseThrow().apply("SELECT `key` FROM `test`",
                        List.of(new JdbcSortItem(column, order)), 2);
                assertThat(topN).contains(
                        "CASE WHEN `key` != `key` THEN 1 ELSE 0 END " + nanDirection,
                        "NANVL(`key`, CAST(0 AS " + nativeType + ")) " + (order.isAscending() ? "ASC" : "DESC"));
            }
        }
    }

    @Test
    public void testUnsupportedProjectionTypesHaveNoMapping() {
        assertThat(YdbTypeUtils.toTypeHandle(new ArrayType(BIGINT))).isEmpty();
        assertThat(YdbTypeUtils.toTypeHandle(createTimestampType(12))).isEmpty();
    }

    @Test
    public void testAutomaticJoinCosts() {
        YdbClient costAwareClient = costAwareClient();
        ConnectorSession session = joinSession(Map.of());
        JdbcColumnHandle key = new JdbcColumnHandle("key", typeHandle("Int64"), BIGINT);
        JdbcJoinCondition condition = new JdbcJoinCondition(key, EQUAL, key);
        for (JoinType type : JoinType.values()) {
            PreparedQuery query = legacyJoin(costAwareClient, session, type, condition, statistics(100L, 100L, 249L)).orElseThrow();
            assertThat(query.query()).contains("JOIN", "l.`key` = r.`key`");
            assertThat(query.parameters()).containsExactly(
                    new QueryParameter(BIGINT, Optional.of(1L)),
                    new QueryParameter(BIGINT, Optional.of(2L)));
            assertThat(legacyJoin(costAwareClient, session, type, condition, statistics(100L, 100L, 250L))).isEmpty();
        }
        for (JoinStatistics statistics : List.of(
                statistics(null, 100L, 10L),
                statistics(100L, null, 10L),
                statistics(100L, 100L, null))) {
            assertThat(legacyJoin(costAwareClient, session, JoinType.INNER, condition, statistics)).isEmpty();
        }
        ConnectorSession limited = joinSession(Map.of("join_pushdown_automatic_max_table_size", "100B"));
        assertThat(legacyJoin(costAwareClient, limited, JoinType.INNER, condition, statistics(101L, 100L, 10L))).isEmpty();
        assertThat(legacyJoin(costAwareClient, limited, JoinType.INNER, condition, statistics(100L, 101L, 10L))).isEmpty();
        assertThat(legacyJoin(costAwareClient, limited, JoinType.INNER, condition, statistics(100L, 100L, 10L))).isPresent();
        ConnectorSession ratio = joinSession(Map.of("join_pushdown_automatic_max_join_to_tables_ratio", 0.5));
        assertThat(legacyJoin(costAwareClient, ratio, JoinType.INNER, condition, statistics(100L, 100L, 100L))).isEmpty();
    }

    @Test
    public void testEagerJoinPreservesKeyRestrictions() {
        ConnectorSession eager = joinSession(Map.of("join_pushdown_strategy", "EAGER"));
        JdbcColumnHandle key = new JdbcColumnHandle("key", typeHandle("Int64"), BIGINT);
        assertThat(legacyJoin(costAwareClient(), eager, JoinType.INNER,
                new JdbcJoinCondition(key, EQUAL, key), statistics(null, null, null))).isPresent();
        for (var operator : List.of(LESS_THAN, IDENTICAL)) {
            assertThat(legacyJoin(client, eager, JoinType.INNER,
                    new JdbcJoinCondition(key, operator, key), statistics(null, null, null))).isEmpty();
        }
        JdbcColumnHandle decimal = new JdbcColumnHandle("key", typeHandle("Decimal(20,0)"), createDecimalType(20, 0));
        assertThat(legacyJoin(client, eager, JoinType.INNER,
                new JdbcJoinCondition(decimal, EQUAL, decimal), statistics(null, null, null))).isEmpty();
    }

    private static YdbClient costAwareClient() {
        return new YdbClient(new BaseJdbcConfig(),
                _ -> (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                        (_, method, _) -> {
                            assertThat(method.getName()).isEqualTo("close");
                            return null;
                        }),
                new YdbQueryBuilder(RemoteQueryModifier.NONE), new DefaultIdentifierMapping(), RemoteQueryModifier.NONE);
    }

    private static ConnectorSession joinSession(Map<String, Object> properties) {
        return TestingConnectorSession.builder()
                .setPropertyMetadata(new JdbcJoinPushdownSessionProperties(new JdbcJoinPushdownConfig()).getSessionProperties())
                .setPropertyValues(properties)
                .build();
    }

    private static Optional<PreparedQuery> legacyJoin(YdbClient client, ConnectorSession session, JoinType type,
            JdbcJoinCondition condition, JoinStatistics statistics) {
        return client.legacyImplementJoin(session, type,
                new PreparedQuery("SELECT `key` FROM `left_table` WHERE `key` > ?", List.of(new QueryParameter(BIGINT, Optional.of(1L)))),
                new PreparedQuery("SELECT `key` FROM `right_table` WHERE `key` > ?", List.of(new QueryParameter(BIGINT, Optional.of(2L)))),
                List.of(condition), Map.of(condition.getRightColumn(), "right_key"), Map.of(condition.getLeftColumn(), "left_key"), statistics);
    }

    private static JoinStatistics statistics(Long leftSize, Long rightSize, Long joinSize) {
        return new JoinStatistics() {
            @Override
            public Optional<BasicRelationStatistics> getLeftStatistics() {
                return Optional.ofNullable(leftSize).map(size -> new BasicRelationStatistics(1, size));
            }

            @Override
            public Optional<BasicRelationStatistics> getRightStatistics() {
                return Optional.ofNullable(rightSize).map(size -> new BasicRelationStatistics(1, size));
            }

            @Override
            public Optional<BasicRelationStatistics> getJoinStatistics() {
                return Optional.ofNullable(joinSize).map(size -> new BasicRelationStatistics(1, size));
            }
        };
    }

    private static JdbcTypeHandle typeHandle(String name) {
        return new JdbcTypeHandle(Types.OTHER, Optional.of(name), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
