package tech.ydb.trino;

import io.trino.plugin.base.mapping.DefaultIdentifierMapping;
import io.trino.plugin.jdbc.BaseJdbcConfig;
import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.plugin.jdbc.JdbcTypeHandle;
import io.trino.plugin.jdbc.QueryParameter;
import io.trino.spi.expression.Call;
import io.trino.spi.expression.Constant;
import io.trino.spi.expression.Variable;
import io.trino.spi.type.Type;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.trino.plugin.jdbc.logging.RemoteQueryModifier.NONE;
import static io.trino.spi.expression.StandardFunctions.ADD_FUNCTION_NAME;
import static io.trino.spi.expression.StandardFunctions.DIVIDE_FUNCTION_NAME;
import static io.trino.spi.expression.StandardFunctions.MULTIPLY_FUNCTION_NAME;
import static io.trino.spi.expression.StandardFunctions.NEGATE_FUNCTION_NAME;
import static io.trino.spi.expression.StandardFunctions.SUBTRACT_FUNCTION_NAME;
import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.DoubleType.DOUBLE;
import static io.trino.spi.type.IntegerType.INTEGER;
import static io.trino.spi.type.SmallintType.SMALLINT;
import static io.trino.spi.type.TinyintType.TINYINT;
import static io.trino.testing.connector.TestingConnectorSession.SESSION;
import static org.assertj.core.api.Assertions.assertThat;

public class TestYdbExpressionRewrites {
    private final YdbClient client = new YdbClient(new BaseJdbcConfig(),
            _ -> { throw new AssertionError("Expression rewrite must not open a connection"); },
            new YdbQueryBuilder(NONE), new DefaultIdentifierMapping(), NONE);

    @Test
    public void testCheckedArithmeticAndParameters() {
        for (Type type : List.of(TINYINT, SMALLINT, INTEGER, BIGINT)) {
            JdbcColumnHandle key = column("key", type);
            for (var operation : List.of(ADD_FUNCTION_NAME, SUBTRACT_FUNCTION_NAME, MULTIPLY_FUNCTION_NAME)) {
                Call expression = new Call(type, operation, List.of(new Variable("key", type), new Constant(2L, type)));
                var rewrite = client.convertPredicate(SESSION, expression, Map.of("key", key)).orElseThrow();
                assertThat(rewrite.expression()).contains("IS NULL", "Decimal(35,0)", "Unwrap", "integer overflow");
                assertThat(rewrite.parameters()).containsExactly(
                        new QueryParameter(type, Optional.of(2L)), new QueryParameter(type, Optional.of(2L)));
            }
            var negate = client.convertPredicate(SESSION,
                    new Call(type, NEGATE_FUNCTION_NAME, List.of(new Variable("key", type))),
                    Map.of("key", key)).orElseThrow();
            assertThat(negate.expression()).contains("-CAST", "Unwrap", "integer overflow");
        }
    }

    @Test
    public void testNestedParameterOrderAndDivisionGuard() {
        JdbcColumnHandle key = column("key", BIGINT);
        Call add = new Call(BIGINT, ADD_FUNCTION_NAME,
                List.of(new Variable("key", BIGINT), new Constant(2L, BIGINT)));
        Call multiply = new Call(BIGINT, MULTIPLY_FUNCTION_NAME, List.of(add, new Constant(3L, BIGINT)));
        assertThat(client.convertPredicate(SESSION, multiply, Map.of("key", key)).orElseThrow().parameters())
                .extracting(parameter -> parameter.getValue().orElseThrow())
                .containsExactly(2L, 2L, 3L, 2L, 2L, 3L);

        for (Constant divisor : List.of(new Constant(-1L, BIGINT), new Constant(0L, BIGINT),
                new Constant(2.0, DOUBLE))) {
            Call divide = new Call(BIGINT, DIVIDE_FUNCTION_NAME, List.of(new Variable("key", BIGINT), divisor));
            assertThat(client.convertPredicate(SESSION, divide, Map.of("key", key))).isEmpty();
        }
    }

    @Test
    public void testWideningCastsOnly() {
        RewriteCast rewrite = new RewriteCast();
        for (Map.Entry<String, List<Type>> entry : Map.<String, List<Type>>of(
                "Int8", List.of(TINYINT, SMALLINT, INTEGER, BIGINT),
                "Uint8", List.of(SMALLINT, INTEGER, BIGINT),
                "Int16", List.of(SMALLINT, INTEGER, BIGINT),
                "Uint16", List.of(INTEGER, BIGINT),
                "Int32", List.of(INTEGER, BIGINT),
                "Uint32", List.of(BIGINT),
                "Int64", List.of(BIGINT),
                "Uint64", List.of()).entrySet()) {
            JdbcTypeHandle source = new JdbcTypeHandle(Types.BIGINT, Optional.of(entry.getKey()),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
            for (Type target : List.of(TINYINT, SMALLINT, INTEGER, BIGINT)) {
                assertThat(rewrite.toJdbcTypeHandle(source, target).isPresent())
                        .as("%s to %s", entry.getKey(), target)
                        .isEqualTo(entry.getValue().contains(target));
            }
        }
    }

    private static JdbcColumnHandle column(String name, Type type) {
        return new JdbcColumnHandle(name, YdbTypeUtils.toTypeHandle(type).orElseThrow(), type);
    }
}
