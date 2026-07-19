package tech.ydb.trino;

import io.trino.matching.Captures;
import io.trino.matching.Pattern;
import io.trino.plugin.base.expression.ConnectorExpressionRule;
import io.trino.plugin.jdbc.expression.ParameterizedExpression;
import io.trino.spi.expression.Call;
import io.trino.spi.expression.StandardFunctions;

import java.util.Optional;

import static io.trino.plugin.base.expression.ConnectorExpressionPatterns.argumentCount;
import static io.trino.plugin.base.expression.ConnectorExpressionPatterns.call;
import static io.trino.plugin.base.expression.ConnectorExpressionPatterns.functionName;
import static java.lang.String.format;

/**
 * Rewrite <code>$nullif(a, b)</code>, as:
 * <br />
 * <code>CASE WHEN a = b THEN NULL ELSE a END</code>.
 */
public class RewriteNullIf implements ConnectorExpressionRule<Call, ParameterizedExpression> {
    private final Pattern<Call> PATTERN;

    public RewriteNullIf() {
        this.PATTERN = call()
                .with(functionName().matching(name -> name.equals(StandardFunctions.NULLIF_FUNCTION_NAME)))
                .with(argumentCount().matching(count -> count == 2));
    }

    @Override
    public Pattern<Call> getPattern() {
        return PATTERN;
    }

    @Override
    public Optional<ParameterizedExpression> rewrite(Call call, Captures captures, RewriteContext<ParameterizedExpression> context) {
        return RewriteUtils.rewriteBinaryExpression(
                call,
                context,
                () -> true,
                (left, right) -> format("CASE WHEN %s = %s THEN NULL ELSE %s END", left, right, left)
        );
    }
}
