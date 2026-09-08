package tech.ydb.trino;

import io.trino.plugin.base.mapping.DefaultIdentifierMapping;
import io.trino.plugin.jdbc.BaseJdbcConfig;
import io.trino.plugin.jdbc.DefaultQueryBuilder;
import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.plugin.jdbc.JdbcTableHandle;
import io.trino.plugin.jdbc.JdbcTypeHandle;
import io.trino.plugin.jdbc.QueryParameter;
import io.trino.plugin.jdbc.RemoteTableName;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.expression.Call;
import io.trino.spi.expression.Constant;
import io.trino.spi.expression.FunctionName;
import io.trino.spi.expression.Variable;
import io.trino.spi.type.Type;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.trino.plugin.jdbc.logging.RemoteQueryModifier.NONE;
import static io.trino.spi.expression.StandardFunctions.ADD_FUNCTION_NAME;
import static io.trino.spi.expression.StandardFunctions.CAST_FUNCTION_NAME;
import static io.trino.spi.expression.StandardFunctions.DIVIDE_FUNCTION_NAME;
import static io.trino.spi.expression.StandardFunctions.EQUAL_OPERATOR_FUNCTION_NAME;
import static io.trino.spi.expression.StandardFunctions.LESS_THAN_OPERATOR_FUNCTION_NAME;
import static io.trino.spi.expression.StandardFunctions.MULTIPLY_FUNCTION_NAME;
import static io.trino.spi.expression.StandardFunctions.NEGATE_FUNCTION_NAME;
import static io.trino.spi.expression.StandardFunctions.SUBTRACT_FUNCTION_NAME;
import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.BooleanType.BOOLEAN;
import static io.trino.spi.type.DoubleType.DOUBLE;
import static io.trino.spi.type.IntegerType.INTEGER;
import static io.trino.spi.type.SmallintType.SMALLINT;
import static io.trino.spi.type.TinyintType.TINYINT;
import static io.trino.spi.type.VarcharType.VARCHAR;
import static io.trino.testing.connector.TestingConnectorSession.SESSION;
import static org.assertj.core.api.Assertions.assertThat;

public class TestYdbExpressionRewrites {
    private final YdbClient client = new YdbClient(new BaseJdbcConfig(),
            _ -> { throw new AssertionError("Expression rewriting must not open a connection"); },
            new DefaultQueryBuilder(NONE), new DefaultIdentifierMapping(), NONE);

    @Test
    public void testWideningCastRanges() {
        RewriteCast rewrite = new RewriteCast();
        for (Map.Entry<String, List<Type>> entry : Map.<String, List<Type>>of(
                "Int8", List.of(TINYINT, SMALLINT, INTEGER, BIGINT),
                "Uint8", List.of(SMALLINT, INTEGER, BIGINT),
                "Int16", List.of(SMALLINT, INTEGER, BIGINT),
                "Uint16", List.of(INTEGER, BIGINT),
                "Int32", List.of(INTEGER, BIGINT),
                "Uint32", List.of(BIGINT),
                "Int64", List.of(BIGINT),
                "Uint64", List.of(),
                "Double", List.of()).entrySet()) {
            JdbcTypeHandle source = new JdbcTypeHandle(Types.BIGINT, Optional.of(entry.getKey()),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
            for (Type target : List.of(TINYINT, SMALLINT, INTEGER, BIGINT, DOUBLE)) {
                assertThat(rewrite.toJdbcTypeHandle(source, target).isPresent())
                        .as("%s to %s", entry.getKey(), target).isEqualTo(entry.getValue().contains(target));
            }
        }
    }

    @Test
    public void testQuotedWideningCastProjection() {
        JdbcColumnHandle column = new JdbcColumnHandle("Mixed Key", YdbTypeUtils.toTypeHandle(INTEGER).orElseThrow(), INTEGER);
        JdbcTableHandle table = new JdbcTableHandle(new SchemaTableName("default", "table"),
                new RemoteTableName(Optional.empty(), Optional.empty(), "table"), Optional.empty());
        Call cast = new Call(BIGINT, CAST_FUNCTION_NAME, List.of(new Variable("key", INTEGER)));
        var projection = client.convertProjection(SESSION, table, cast, Map.of("key", column)).orElseThrow();
        assertThat(projection.getExpression()).isEqualTo("CAST(`Mixed Key` AS Int64)");
        assertThat(projection.getParameters()).isEmpty();
        assertThat(projection.getJdbcTypeHandle()).isEqualTo(YdbTypeUtils.toTypeHandle(BIGINT).orElseThrow());
    }

    @Test
    public void testCheckedArithmeticParameters() {
        for (Type type : List.of(TINYINT, SMALLINT, INTEGER, BIGINT)) {
            JdbcColumnHandle column = new JdbcColumnHandle("key", YdbTypeUtils.toTypeHandle(type).orElseThrow(), type);
            for (var operation : List.of(ADD_FUNCTION_NAME, SUBTRACT_FUNCTION_NAME, MULTIPLY_FUNCTION_NAME)) {
                Call expression = new Call(type, operation, List.of(new Variable("key", type), new Constant(2L, type)));
                var rewrite = client.convertPredicate(SESSION, expression, Map.of("key", column)).orElseThrow();
                assertThat(rewrite.parameters()).containsExactly(
                        new QueryParameter(type, Optional.of(2L)), new QueryParameter(type, Optional.of(2L)));
                assertThat(rewrite.expression()).contains("IS NULL", "Decimal(35,0)", "Unwrap", "AS " +
                        YdbTypeUtils.toTypeHandle(type).orElseThrow().jdbcTypeName().orElseThrow());
            }
            var negate = client.convertPredicate(SESSION,
                    new Call(type, NEGATE_FUNCTION_NAME, List.of(new Variable("key", type))), Map.of("key", column)).orElseThrow();
            assertThat(negate.expression()).contains("IF", "-CAST", "Unwrap");
        }
    }

    @Test
    public void testNestedArithmeticParameterOrder() {
        JdbcColumnHandle column = new JdbcColumnHandle("key", YdbTypeUtils.toTypeHandle(BIGINT).orElseThrow(), BIGINT);
        Call add = new Call(BIGINT, ADD_FUNCTION_NAME, List.of(new Variable("key", BIGINT), new Constant(2L, BIGINT)));
        Call multiply = new Call(BIGINT, MULTIPLY_FUNCTION_NAME, List.of(add, new Constant(3L, BIGINT)));
        assertThat(client.convertPredicate(SESSION, multiply, Map.of("key", column)).orElseThrow().parameters())
                .extracting(parameter -> parameter.getValue().orElseThrow())
                .containsExactly(2L, 2L, 3L, 2L, 2L, 3L);
    }

    @Test
    public void testDivisionRejectsOverflowAndMismatchedConstants() {
        JdbcColumnHandle column = new JdbcColumnHandle("key", YdbTypeUtils.toTypeHandle(BIGINT).orElseThrow(), BIGINT);
        for (Constant divisor : List.of(new Constant(-1L, BIGINT), new Constant(0L, BIGINT), new Constant(2.0, DOUBLE))) {
            Call divide = new Call(BIGINT, DIVIDE_FUNCTION_NAME, List.of(new Variable("key", BIGINT), divisor));
            assertThat(client.convertPredicate(SESSION, divide, Map.of("key", column))).isEmpty();
        }
        Call divide = new Call(BIGINT, DIVIDE_FUNCTION_NAME, List.of(new Variable("key", BIGINT), new Constant(-2L, BIGINT)));
        assertThat(client.convertPredicate(SESSION, divide, Map.of("key", column))).isPresent();
    }

    @Test
    public void testLossyStringExpressionsStayLocal() {
        JdbcTableHandle table = new JdbcTableHandle(new SchemaTableName("default", "table"),
                new RemoteTableName(Optional.empty(), Optional.empty(), "table"), Optional.empty());
        Variable key = new Variable("key", VARCHAR);
        Call concat = new Call(VARCHAR, new FunctionName("$concat"), List.of(key, key));
        Call equal = new Call(BOOLEAN, EQUAL_OPERATOR_FUNCTION_NAME, List.of(key, key));
        for (String type : List.of("String", "Bytes", "Utf8")) {
            JdbcColumnHandle column = new JdbcColumnHandle("key", new JdbcTypeHandle(Types.VARCHAR,
                    Optional.of(type), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()), VARCHAR);
            assertThat(client.convertProjection(SESSION, table, concat, Map.of("key", column)).isPresent())
                    .as("%s concat", type).isEqualTo(type.equals("Utf8"));
            assertThat(client.convertPredicate(SESSION, equal, Map.of("key", column)).isPresent())
                    .as("%s equality", type).isEqualTo(type.equals("Utf8"));
        }
    }

    @Test
    public void testUnsignedBigintExpressionsUseJdbcValue() {
        Variable key = new Variable("key", BIGINT);
        for (String type : List.of("Uint64", "Int64")) {
            JdbcColumnHandle column = new JdbcColumnHandle("Mixed Key", new JdbcTypeHandle(Types.BIGINT,
                    Optional.of(type), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()), BIGINT);
            String reference = type.equals("Uint64") ? "BITCAST(`Mixed Key` AS Int64)" : "`Mixed Key`";
            Call predicate = new Call(BOOLEAN, LESS_THAN_OPERATOR_FUNCTION_NAME, List.of(key, new Constant(0L, BIGINT)));
            var comparison = client.convertPredicate(SESSION, predicate, Map.of("key", column)).orElseThrow();
            assertThat(comparison.expression()).isEqualTo("(" + reference + ") < (?)");
            assertThat(comparison.parameters()).containsExactly(new QueryParameter(BIGINT, Optional.of(0L)));

            Call add = new Call(BIGINT, ADD_FUNCTION_NAME, List.of(key, new Constant(2L, BIGINT)));
            var arithmetic = client.convertPredicate(SESSION, add, Map.of("key", column)).orElseThrow();
            assertThat(arithmetic.expression()).contains("(" + reference + ") IS NULL", "CAST((" + reference + ") AS Decimal(35,0))");
            assertThat(arithmetic.parameters()).containsExactly(
                    new QueryParameter(BIGINT, Optional.of(2L)), new QueryParameter(BIGINT, Optional.of(2L)));

            Call divide = new Call(BIGINT, DIVIDE_FUNCTION_NAME, List.of(key, new Constant(2L, BIGINT)));
            var division = client.convertPredicate(SESSION, divide, Map.of("key", column)).orElseThrow();
            assertThat(division.expression()).isEqualTo("(" + reference + ") / (?)");
            assertThat(division.parameters()).containsExactly(new QueryParameter(BIGINT, Optional.of(2L)));
        }
    }
}
