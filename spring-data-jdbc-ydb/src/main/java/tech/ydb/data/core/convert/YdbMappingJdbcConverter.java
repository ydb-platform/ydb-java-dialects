package tech.ydb.data.core.convert;

import java.sql.SQLType;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.data.convert.CustomConversions;
import org.springframework.data.jdbc.core.convert.JdbcTypeFactory;
import org.springframework.data.jdbc.core.convert.MappingJdbcConverter;
import org.springframework.data.jdbc.core.convert.RelationResolver;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import tech.ydb.data.core.convert.annotation.YdbType;


/**
 * @author Madiyar Nurgazin
 * @author Mikhail Polivakha
 */
@SuppressWarnings("removal")
public class YdbMappingJdbcConverter extends MappingJdbcConverter {
    private final static Class<YdbType> ANNOTATION = YdbType.class;
    private final static Class<tech.ydb.data.core.convert.YdbType> OLD_TYPE = tech.ydb.data.core.convert.YdbType.class;

    private final ConcurrentMap<RelationalPersistentProperty, SQLType> typesCache = new ConcurrentHashMap<>();

    public YdbMappingJdbcConverter(RelationalMappingContext context, RelationResolver relationResolver,
                                   CustomConversions conversions, JdbcTypeFactory typeFactory) {
        super(context, relationResolver, conversions, typeFactory);
    }

    @Override
    public SQLType getTargetSqlType(RelationalPersistentProperty property) {
         return typesCache.computeIfAbsent(property, this::resolveSqlType);
    }

    private SQLType resolveSqlType(RelationalPersistentProperty property) {
        if (property.isAnnotationPresent(ANNOTATION)) {
            tech.ydb.data.core.convert.annotation.YdbType type = property.getRequiredAnnotation(ANNOTATION);
            YQLType yql = type.value();
            if (yql == YQLType.Decimal) {
                int precision = type.decimalPrecision();
                int scale = type.decimalScale();
                return new YdbSqlType(precision, scale);
            }
            return new YdbSqlType(yql);
        }

        if (property.isAnnotationPresent(OLD_TYPE)) {
            String typeName = property.getRequiredAnnotation(OLD_TYPE).value();
            return new YdbSqlType(YQLType.valueOf(typeName));
        }

        return super.getTargetSqlType(property);
    }
}
