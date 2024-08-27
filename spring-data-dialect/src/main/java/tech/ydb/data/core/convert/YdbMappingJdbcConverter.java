package tech.ydb.data.core.convert;

import java.sql.SQLType;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.jdbc.core.convert.JdbcTypeFactory;
import org.springframework.data.jdbc.core.convert.MappingJdbcConverter;
import org.springframework.data.jdbc.core.convert.RelationResolver;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import tech.ydb.table.values.PrimitiveType;

/**
 * @author Madiyar Nurgazin
 */
public class YdbMappingJdbcConverter extends MappingJdbcConverter {
    public YdbMappingJdbcConverter(RelationalMappingContext context, RelationResolver relationResolver,
                                   CustomConversions conversions, JdbcTypeFactory typeFactory) {
        super(context, relationResolver, conversions, typeFactory);
    }

    @Override
    public SQLType getTargetSqlType(RelationalPersistentProperty property) {
        return property.isAnnotationPresent(YdbType.class) ?
                new YQLType(PrimitiveType.valueOf(property.getRequiredAnnotation(YdbType.class).value())) :
                super.getTargetSqlType(property);
    }
}
