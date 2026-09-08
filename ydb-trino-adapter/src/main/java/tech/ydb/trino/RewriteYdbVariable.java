package tech.ydb.trino;

import io.trino.matching.Captures;
import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.plugin.jdbc.expression.ParameterizedExpression;
import io.trino.plugin.jdbc.expression.RewriteVariable;
import io.trino.spi.expression.Variable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.trino.spi.type.BigintType.BIGINT;
import static java.util.Objects.requireNonNull;

public class RewriteYdbVariable extends RewriteVariable {
    private final Predicate<JdbcColumnHandle> supportedMapping;

    public RewriteYdbVariable(Function<String, String> quote, Predicate<JdbcColumnHandle> supportedMapping) {
        super(quote);
        this.supportedMapping = requireNonNull(supportedMapping, "supportedMapping is null");
    }

    @Override
    public Optional<ParameterizedExpression> rewrite(Variable variable, Captures captures,
            RewriteContext<ParameterizedExpression> context) {
        JdbcColumnHandle column = (JdbcColumnHandle) context.getAssignment(variable.getName());
        if (!supportedMapping.test(column)) {
            return Optional.empty();
        }
        Optional<ParameterizedExpression> expression = super.rewrite(variable, captures, context);
        if (column.getColumnType().equals(BIGINT)
                && column.getJdbcTypeHandle().jdbcTypeName().filter("Uint64"::equalsIgnoreCase).isPresent()) {
            // JDBC exposes Uint64 through its signed long bit pattern; use that same value in expressions.
            return expression.map(value -> new ParameterizedExpression(
                    "BITCAST(" + value.expression() + " AS Int64)", value.parameters()));
        }
        return expression;
    }
}
