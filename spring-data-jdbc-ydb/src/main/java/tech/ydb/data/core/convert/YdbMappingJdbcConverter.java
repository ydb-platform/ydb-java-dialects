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
 * @author Mikhail Polivakha
 */
@SuppressWarnings("removal")
public class YdbMappingJdbcConverter extends MappingJdbcConverter {

    public YdbMappingJdbcConverter(RelationalMappingContext context, RelationResolver relationResolver,
                                   CustomConversions conversions, JdbcTypeFactory typeFactory) {
        super(context, relationResolver, conversions, typeFactory);
    }

    @Override
    public SQLType getTargetSqlType(RelationalPersistentProperty property) {
        // the new api takes precedence
        var ydbType = property.findAnnotation(tech.ydb.data.core.convert.annotation.YdbType.class);

        if (ydbType != null) {
            return new YQLType(ydbType.value());
        }

        YdbType oldType = property.findAnnotation(YdbType.class);

        if (oldType != null) {
            return new YQLType(PrimitiveType.valueOf(oldType.value()));
        }

        return super.getTargetSqlType(property);
    }
}
