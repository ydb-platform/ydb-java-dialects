package tech.ydb.trino;

import io.trino.matching.Captures;
import io.trino.matching.Pattern;
import io.trino.plugin.base.expression.ConnectorExpressionRule;
import io.trino.plugin.jdbc.QueryParameter;
import io.trino.plugin.jdbc.expression.ParameterizedExpression;
import io.trino.spi.expression.Constant;

import java.util.List;
import java.util.Optional;

import static io.trino.plugin.base.expression.ConnectorExpressionPatterns.constant;
import static io.trino.plugin.base.expression.ConnectorExpressionPatterns.type;
import static io.trino.spi.type.VarbinaryType.VARBINARY;

public class RewriteVarbinaryConstant implements ConnectorExpressionRule<Constant, ParameterizedExpression> {
    @Override
    public Pattern<Constant> getPattern() {
        return constant().with(type().equalTo(VARBINARY));
    }

    @Override
    public Optional<ParameterizedExpression> rewrite(Constant constant, Captures captures,
            RewriteContext<ParameterizedExpression> context) {
        return Optional.of(new ParameterizedExpression("?",
                List.of(new QueryParameter(VARBINARY, Optional.ofNullable(constant.getValue())))));
    }
}
