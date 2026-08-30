package tech.ydb.trino;

import com.google.inject.Inject;
import io.trino.plugin.jdbc.DefaultQueryBuilder;
import io.trino.plugin.jdbc.JdbcClient;
import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.plugin.jdbc.PreparedQuery;
import io.trino.plugin.jdbc.expression.ParameterizedExpression;
import io.trino.plugin.jdbc.logging.RemoteQueryModifier;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.JoinType;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Verify.verify;
import static java.util.stream.Collectors.toList;

public class YdbQueryBuilder extends DefaultQueryBuilder {
    // `something` или (`something`)
    private static final Pattern COLUMN_REF_PATTERN = Pattern.compile("\\(?`([^`]+)`\\)?");

    @Inject
    public YdbQueryBuilder(RemoteQueryModifier queryModifier)
    {
        super(queryModifier);
    }

    @Override
    public PreparedQuery prepareJoinQuery(
            JdbcClient client,
            ConnectorSession session,
            Connection connection,
            JoinType joinType,
            PreparedQuery leftSource,
            Map<JdbcColumnHandle, String> leftProjections,
            PreparedQuery rightSource,
            Map<JdbcColumnHandle, String> rightProjections,
            List<ParameterizedExpression> joinConditions)
    {
        List<ParameterizedExpression> qualifiedConditions = joinConditions.stream()
                .map(this::qualifyJoinCondition)
                .collect(toList());

        return super.prepareJoinQuery(client, session, connection, joinType,
                leftSource, leftProjections, rightSource, rightProjections, qualifiedConditions);
    }

    private ParameterizedExpression qualifyJoinCondition(ParameterizedExpression condition) {
        String expression = condition.expression();

        String[] parts = expression.split(" = ");
        if (parts.length == 2) {
            String left = parts[0].trim();
            String right = parts[1].trim();

            left = extractColumnRef(left);
            right = extractColumnRef(right);

            String qualifiedExpression = "l." + left + " = r." + right;
            return new ParameterizedExpression(qualifiedExpression, condition.parameters());
        }

        return condition;
    }

    // (`something`) -> `something`
    private String extractColumnRef(String expr)
    {
        Matcher matcher = COLUMN_REF_PATTERN.matcher(expr);
        if (matcher.find()) {
            return "`" + matcher.group(1) + "`";
        }
        return expr;
    }
}
