package tech.ydb.data.core.convert;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.sql.SqlIdentifier;

/**
 * Computes the set of id columns and updatable (non-id, writable, non-insert-only) columns for an entity,
 * correctly propagating "is part of the id" into columns reached through a composite/embedded {@code @Id}.
 * <p>
 * This mirrors {@code org.springframework.data.jdbc.core.convert.SqlGenerator.Columns}, which has a known gap for
 * composite ids: {@code initSimpleColumnName} checks {@code property.getOwner().isIdProperty(property)}, and for a
 * property reached by recursing into an embedded id, the "owner" is the id wrapper type itself, which has no
 * {@code @Id} field of its own - so none of the composite id's columns are recognized as id columns and all end up
 * in the {@code UPDATE ... SET ...} clause. YDB rejects such statements with "Cannot update primary key column"
 * (see YDBREQUESTS-8347, https://github.com/spring-projects/spring-data-relational/issues/2338).
 */
final class YdbColumns {

    private final Set<SqlIdentifier> idColumns = new LinkedHashSet<>();
    private final Set<SqlIdentifier> updatableColumns = new LinkedHashSet<>();

    private YdbColumns() {
    }

    static YdbColumns resolve(RelationalPersistentEntity<?> entity, RelationalMappingContext context,
            JdbcConverter converter) {

        YdbColumns columns = new YdbColumns();
        columns.collect(entity, context, converter, "", false);
        return columns;
    }

    Set<SqlIdentifier> getIdColumns() {
        return idColumns;
    }

    Set<SqlIdentifier> getUpdatableColumns() {
        return updatableColumns;
    }

    private void collect(RelationalPersistentEntity<?> entity, RelationalMappingContext context,
            JdbcConverter converter, String prefix, boolean withinId) {

        entity.doWithAll(property -> {

            boolean propertyIsId = withinId || entity.isIdProperty(property);

            if (!property.isEntity()) {

                SqlIdentifier columnName = property.getColumnName().transform(prefix::concat);

                if (propertyIsId) {
                    idColumns.add(columnName);
                } else if (property.isWritable() && !property.isInsertOnly()) {
                    updatableColumns.add(columnName);
                }
            } else if (property.isEmbedded()) {

                RelationalPersistentEntity<?> embeddedEntity = context
                        .getRequiredPersistentEntity(converter.getColumnType(property));

                collect(embeddedEntity, context, converter, prefix + property.getEmbeddedPrefix(), propertyIsId);
            }
        });
    }
}
