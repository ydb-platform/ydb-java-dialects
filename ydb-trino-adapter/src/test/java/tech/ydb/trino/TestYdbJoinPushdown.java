package tech.ydb.trino;

import io.trino.plugin.base.mapping.DefaultIdentifierMapping;
import io.trino.plugin.jdbc.BaseJdbcConfig;
import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.plugin.jdbc.JdbcJoinCondition;
import io.trino.plugin.jdbc.JdbcTypeHandle;
import io.trino.plugin.jdbc.PreparedQuery;
import io.trino.plugin.jdbc.QueryParameter;
import io.trino.spi.connector.JoinType;
import io.trino.spi.type.Type;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.trino.plugin.jdbc.logging.RemoteQueryModifier.NONE;
import static io.trino.spi.connector.JoinCondition.Operator.EQUAL;
import static io.trino.spi.connector.JoinCondition.Operator.LESS_THAN;
import static io.trino.spi.connector.JoinType.INNER;
import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.DoubleType.DOUBLE;
import static io.trino.spi.type.RealType.REAL;
import static io.trino.spi.type.VarbinaryType.VARBINARY;
import static io.trino.testing.connector.TestingConnectorSession.SESSION;
import static org.assertj.core.api.Assertions.assertThat;

public class TestYdbJoinPushdown {
    private final YdbQueryBuilder queryBuilder = new YdbQueryBuilder(NONE);
    private final YdbClient client = new YdbClient(new BaseJdbcConfig(),
            _ -> { throw new AssertionError("JOIN admission must not open a connection"); },
            queryBuilder, new DefaultIdentifierMapping(), NONE);

    @Test
    public void testQualifiedKeysAndSourceParameterOrder() {
        JdbcColumnHandle key = column("join key", BIGINT);
        QueryParameter leftParameter = new QueryParameter(BIGINT, Optional.of(11L));
        QueryParameter rightParameter = new QueryParameter(BIGINT, Optional.of(21L));
        for (JoinType joinType : JoinType.values()) {
            PreparedQuery query = queryBuilder.legacyPrepareJoinQuery(
                    client, SESSION, null, joinType,
                    new PreparedQuery("SELECT `join key` FROM `left` WHERE id > ?", List.of(leftParameter)),
                    new PreparedQuery("SELECT `join key` FROM `right` WHERE id > ?", List.of(rightParameter)),
                    List.of(new JdbcJoinCondition(key, EQUAL, key)),
                    Map.of(key, "left_key"), Map.of(key, "right_key"));
            assertThat(query.query()).contains("l.`join key` AS `left_key`", "r.`join key` AS `right_key`",
                    "ON l.`join key` = r.`join key`");
            assertThat(query.parameters()).containsExactly(leftParameter, rightParameter);
        }
    }

    @Test
    public void testMultipleJoinKeys() {
        JdbcColumnHandle first = column("first", BIGINT);
        JdbcColumnHandle second = column("second", BIGINT);
        PreparedQuery query = queryBuilder.legacyPrepareJoinQuery(
                client, SESSION, null, INNER,
                new PreparedQuery("SELECT `first`, `second` FROM `left`", List.of()),
                new PreparedQuery("SELECT `first`, `second` FROM `right`", List.of()),
                List.of(new JdbcJoinCondition(first, EQUAL, second), new JdbcJoinCondition(second, EQUAL, first)),
                Map.of(first, "left_first", second, "left_second"),
                Map.of(first, "right_first", second, "right_second"));
        assertThat(query.query()).contains("ON l.`first` = r.`second` AND l.`second` = r.`first`");
    }

    @Test
    public void testFloatingAndUnsignedNormalization() {
        for (Type type : List.of(REAL, DOUBLE)) {
            JdbcColumnHandle key = column("join key", type);
            String sqlType = type.equals(REAL) ? "Float" : "Double";
            assertThat(queryBuilder.formatJoinCondition(client, "l", "r", new JdbcJoinCondition(key, EQUAL, key)))
                    .contains("NANVL(IF(COALESCE(l.`join key` = 0, false), CAST(0 AS " + sqlType,
                            "NANVL(IF(COALESCE(r.`join key` = 0, false), CAST(0 AS " + sqlType,
                            "CAST(NULL AS " + sqlType + "))");
        }
        JdbcColumnHandle unsigned = nativeColumn("join key", "Uint64", BIGINT);
        JdbcColumnHandle signed = column("join key", BIGINT);
        assertThat(queryBuilder.formatJoinCondition(client, "l", "r", new JdbcJoinCondition(unsigned, EQUAL, signed)))
                .isEqualTo("BITCAST(l.`join key` AS Int64) = r.`join key`");
        assertThat(queryBuilder.formatJoinCondition(client, "l", "r", new JdbcJoinCondition(signed, EQUAL, unsigned)))
                .isEqualTo("l.`join key` = BITCAST(r.`join key` AS Int64)");
    }

    @Test
    public void testAdmissionGuards() {
        JdbcColumnHandle bigint = column("key", BIGINT);
        JdbcColumnHandle synthetic = JdbcColumnHandle.builderFrom(bigint)
                .setComment(Optional.of("synthetic"))
                .build();
        assertThat(client.isSupportedJoinCondition(SESSION, new JdbcJoinCondition(bigint, EQUAL, synthetic))).isTrue();
        for (String binaryType : List.of("String", "Bytes")) {
            JdbcColumnHandle binary = nativeColumn("key", binaryType, VARBINARY);
            assertThat(client.isSupportedJoinCondition(SESSION, new JdbcJoinCondition(binary, EQUAL, binary))).isTrue();
        }
        assertThat(client.isSupportedJoinCondition(SESSION, new JdbcJoinCondition(bigint, LESS_THAN, bigint))).isFalse();
        JdbcColumnHandle unknown = nativeColumn("key", "Json", BIGINT);
        assertThat(client.isSupportedJoinCondition(SESSION, new JdbcJoinCondition(bigint, EQUAL, unknown))).isFalse();

        YdbClient forced = new YdbClient(new BaseJdbcConfig().setJdbcTypesMappedToVarchar(Set.of("Int64")),
                _ -> { throw new AssertionError("Rejected JOIN must not open a connection"); },
                queryBuilder, new DefaultIdentifierMapping(), NONE);
        assertThat(forced.isSupportedJoinCondition(SESSION, new JdbcJoinCondition(bigint, EQUAL, bigint))).isFalse();
    }

    private static JdbcColumnHandle column(String name, Type type) {
        return new JdbcColumnHandle(name, YdbTypeUtils.toTypeHandle(type).orElseThrow(), type);
    }

    private static JdbcColumnHandle nativeColumn(String name, String jdbcTypeName, Type type) {
        JdbcTypeHandle handle = new JdbcTypeHandle(Types.BIGINT, Optional.of(jdbcTypeName),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        return new JdbcColumnHandle(name, handle, type);
    }
}
