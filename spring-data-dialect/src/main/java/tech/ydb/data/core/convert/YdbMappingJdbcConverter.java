package tech.ydb.data.core.convert;

import java.sql.SQLType;

import org.springframework.data.convert.CustomConversions;
import org.springframework.data.jdbc.core.convert.JdbcTypeFactory;
import org.springframework.data.jdbc.core.convert.MappingJdbcConverter;
import org.springframework.data.jdbc.core.convert.RelationResolver;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import tech.ydb.data.support.YdbJdbcUtil;

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
        return YdbJdbcUtil.targetSqlTypeFor(getColumnType(property));
    }

    @Override
    public Class<?> getColumnType(RelationalPersistentProperty property) {
        if (property.isAssociation()) {
            return getReferenceColumnType(property);
        }

        if (property.isEntity()) {
            Class<?> columnType = getEntityColumnType(property.getTypeInformation().getActualType());

            if (columnType != null) {
                return columnType;
            }
        }

        Class<?> componentColumnType = YdbJdbcColumnTypes.INSTANCE.resolvePrimitiveType(property.getActualType());

        while (componentColumnType.isArray()) {
            componentColumnType = componentColumnType.getComponentType();
        }

        if (property.isCollectionLike() && !property.isEntity()) {
            return java.lang.reflect.Array.newInstance(componentColumnType, 0).getClass();
        }

        return componentColumnType;
    }

    private Class<?> getReferenceColumnType(RelationalPersistentProperty property) {

        Class<?> componentType = property.getTypeInformation().getRequiredComponentType().getType();
        RelationalPersistentEntity<?> referencedEntity = getMappingContext().getRequiredPersistentEntity(componentType);

        return getColumnType(referencedEntity.getRequiredIdProperty());
    }

    @Nullable
    private Class<?> getEntityColumnType(TypeInformation<?> type) {

        RelationalPersistentEntity<?> persistentEntity = getMappingContext().getPersistentEntity(type);

        if (persistentEntity == null) {
            return null;
        }

        RelationalPersistentProperty idProperty = persistentEntity.getIdProperty();

        if (idProperty == null) {
            return null;
        }
        return getColumnType(idProperty);
    }
}
