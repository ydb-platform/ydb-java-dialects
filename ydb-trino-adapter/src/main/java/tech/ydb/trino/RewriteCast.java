package tech.ydb.trino;

import io.trino.plugin.jdbc.JdbcTypeHandle;
import io.trino.plugin.jdbc.expression.AbstractRewriteCast;
import io.trino.spi.type.Type;

import java.util.Locale;
import java.util.Optional;

public class RewriteCast extends AbstractRewriteCast {
    public RewriteCast() {
        super((_, type) -> YdbTypeUtils.toTypeHandle(type).orElseThrow().jdbcTypeName().orElseThrow());
    }

    @Override
    protected Optional<JdbcTypeHandle> toJdbcTypeHandle(JdbcTypeHandle sourceType, Type targetType) {
        // Require the entire physical source range to fit: YQL narrowing casts return NULL instead of failing.
        // https://ydb.tech/docs/en/yql/reference/syntax/expressions#cast
        int sourceBits = switch (sourceType.jdbcTypeName().orElse("").toLowerCase(Locale.ROOT)) {
            case "int8" -> 8;
            case "uint8" -> 9;
            case "int16" -> 16;
            case "uint16" -> 17;
            case "int32" -> 32;
            case "uint32" -> 33;
            case "int64" -> 64;
            default -> Integer.MAX_VALUE;
        };
        int targetBits = switch (targetType.getDisplayName()) {
            case "tinyint" -> 8;
            case "smallint" -> 16;
            case "integer" -> 32;
            case "bigint" -> 64;
            default -> 0;
        };
        return sourceBits <= targetBits ? YdbTypeUtils.toTypeHandle(targetType) : Optional.empty();
    }
}
