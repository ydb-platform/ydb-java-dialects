package tech.ydb.trino;

import com.google.common.collect.ImmutableMap;
import io.trino.plugin.base.mapping.DefaultIdentifierMapping;
import io.trino.plugin.jdbc.BaseJdbcConfig;
import io.trino.plugin.jdbc.DefaultQueryBuilder;
import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.plugin.jdbc.JdbcJoinCondition;
import io.trino.plugin.jdbc.JdbcTypeHandle;
import io.trino.plugin.jdbc.PreparedQuery;
import io.trino.plugin.jdbc.QueryParameter;
import io.trino.plugin.jdbc.expression.ParameterizedExpression;
import io.trino.spi.connector.JoinCondition.Operator;
import io.trino.spi.connector.JoinType;
import io.trino.spi.type.Type;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.trino.plugin.jdbc.logging.RemoteQueryModifier.NONE;
import static io.trino.spi.connector.JoinCondition.Operator.EQUAL;
import static io.trino.spi.connector.JoinCondition.Operator.LESS_THAN;
import static io.trino.spi.connector.JoinType.INNER;
import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.DoubleType.DOUBLE;
import static io.trino.spi.type.IntegerType.INTEGER;
import static io.trino.spi.type.VarcharType.VARCHAR;
import static io.trino.testing.connector.TestingConnectorSession.SESSION;
import static org.assertj.core.api.Assertions.assertThat;

public class TestYdbJoinPushdown {
    private final DefaultQueryBuilder queryBuilder = new DefaultQueryBuilder(NONE);
    private final YdbClient client = new YdbClient(
            new BaseJdbcConfig(),
            _ -> { throw new AssertionError("Rejected JOIN must not open a connection"); },
            queryBuilder,
            new DefaultIdentifierMapping(),
            NONE);

    @Test
    public void testJoinTypesAndQuotedColumns() {
        JdbcColumnHandle key = column("join key", BIGINT);
        for (JoinType joinType : JoinType.values()) {
            String sqlJoinType = switch (joinType) {
                case INNER -> "INNER";
                case LEFT_OUTER -> "LEFT";
                case RIGHT_OUTER -> "RIGHT";
                case FULL_OUTER -> "FULL";
            };
            PreparedQuery query = queryBuilder.legacyPrepareJoinQuery(
                    client, SESSION, null, joinType,
                    new PreparedQuery("SELECT `join key` FROM `left table`", List.of()),
                    new PreparedQuery("SELECT `join key` FROM `right table`", List.of()),
                    List.of(new JdbcJoinCondition(key, EQUAL, key)),
                    Map.of(key, "left key"), Map.of(key, "right key"));

            assertThat(query.query()).isEqualTo(
                    "SELECT l.`join key` AS `left key`, r.`join key` AS `right key` " +
                    "FROM (SELECT `join key` FROM `left table`) l " + sqlJoinType + " JOIN " +
                    "(SELECT `join key` FROM `right table`) r ON l.`join key` = r.`join key`");
            assertThat(query.parameters()).isEmpty();
        }
    }

    @Test
    public void testMultipleKeysAndSourceParameterOrder() {
        JdbcColumnHandle first = column("first", BIGINT);
        JdbcColumnHandle second = column("second", BIGINT);
        QueryParameter leftFirst = new QueryParameter(BIGINT, Optional.of(11L));
        QueryParameter leftSecond = new QueryParameter(BIGINT, Optional.of(12L));
        QueryParameter right = new QueryParameter(BIGINT, Optional.of(21L));
        PreparedQuery query = queryBuilder.legacyPrepareJoinQuery(
                client, SESSION, null, INNER,
                new PreparedQuery("SELECT `first`, `second` FROM `left` WHERE `first` > ? AND `second` < ?",
                        List.of(leftFirst, leftSecond)),
                new PreparedQuery("SELECT `first`, `second` FROM `right` WHERE `first` > ?", List.of(right)),
                List.of(new JdbcJoinCondition(first, EQUAL, second), new JdbcJoinCondition(second, EQUAL, first)),
                ImmutableMap.of(first, "left_first", second, "left_second"),
                ImmutableMap.of(first, "right_first", second, "right_second"));

        assertThat(query.query()).contains(
                "SELECT l.`first` AS `left_first`, l.`second` AS `left_second`, " +
                        "r.`first` AS `right_first`, r.`second` AS `right_second`",
                "ON l.`first` = r.`second` AND l.`second` = r.`first`");
        assertThat(query.parameters()).containsExactly(leftFirst, leftSecond, right);
    }

    @Test
    public void testInt64EqualityAccepted() {
        JdbcColumnHandle key = column("key", BIGINT);
        JdbcColumnHandle nonNullable = JdbcColumnHandle.builderFrom(key).setNullable(false).build();
        assertThat(client.isSupportedJoinCondition(SESSION, new JdbcJoinCondition(key, EQUAL, nonNullable))).isTrue();
        assertThat(client.isSupportedJoinCondition(SESSION,
                new JdbcJoinCondition(key, EQUAL, bigintColumn(Optional.of("iNt64"))))).isTrue();
    }

    @Test
    public void testOtherOperatorsRejected() {
        JdbcColumnHandle key = column("key", BIGINT);
        for (Operator operator : Operator.values()) {
            assertThat(client.isSupportedJoinCondition(SESSION, new JdbcJoinCondition(key, operator, key)))
                    .as("%s", operator).isEqualTo(operator == EQUAL);
        }
    }

    @Test
    public void testOtherTypesAndSyntheticKeysRejected() {
        JdbcColumnHandle key = column("key", BIGINT);
        for (JdbcColumnHandle unsupported : List.of(
                column("key", INTEGER), column("key", DOUBLE), column("key", VARCHAR),
                bigintColumn(Optional.of("Uint32")), bigintColumn(Optional.of("Uint64")),
                bigintColumn(Optional.empty()),
                JdbcColumnHandle.builderFrom(key).setComment(Optional.of("synthetic")).build())) {
            assertThat(client.isSupportedJoinCondition(SESSION, new JdbcJoinCondition(key, EQUAL, unsupported)))
                    .as("right key %s", unsupported).isFalse();
            assertThat(client.isSupportedJoinCondition(SESSION, new JdbcJoinCondition(unsupported, EQUAL, key)))
                    .as("left key %s", unsupported).isFalse();
        }
    }

    @Test
    public void testModernJoinRejectedWithoutConnection() {
        JdbcColumnHandle key = column("key", BIGINT);
        PreparedQuery source = new PreparedQuery("SELECT `key` FROM `table`", List.of());
        assertThat(client.implementJoin(
                SESSION, INNER, source, Map.of(key, "left_key"), source, Map.of(key, "right_key"),
                List.of(new ParameterizedExpression("`left_key` = `right_key`", List.of())), null)).isEmpty();
    }

    @Test
    public void testUnsupportedLegacyJoinRejectedWithoutConnection() {
        JdbcColumnHandle key = column("key", BIGINT);
        PreparedQuery source = new PreparedQuery("SELECT `key` FROM `table`", List.of());
        for (JdbcJoinCondition condition : List.of(
                new JdbcJoinCondition(key, LESS_THAN, key),
                new JdbcJoinCondition(key, EQUAL, bigintColumn(Optional.of("Uint64"))))) {
            assertThat(client.legacyImplementJoin(
                    SESSION, INNER, source, source, List.of(condition),
                    Map.of(key, "right_key"), Map.of(key, "left_key"), null)).isEmpty();
        }
    }

    private static JdbcColumnHandle column(String name, Type type) {
        return new JdbcColumnHandle(name, YdbTypeUtils.toTypeHandle(type).orElseThrow(), type);
    }

    private static JdbcColumnHandle bigintColumn(Optional<String> typeName) {
        return new JdbcColumnHandle("key",
                new JdbcTypeHandle(Types.BIGINT, typeName, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                BIGINT);
    }
}
