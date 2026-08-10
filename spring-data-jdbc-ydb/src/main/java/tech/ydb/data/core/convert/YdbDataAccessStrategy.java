package tech.ydb.data.core.convert;

import java.sql.SQLType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.DelegatingDataAccessStrategy;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.mapping.JdbcValue;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.RenderContextFactory;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.AssignValue;
import org.springframework.data.relational.core.sql.Assignments;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.Update;
import org.springframework.data.relational.core.sql.render.SqlRenderer;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * Decorates the default {@link DataAccessStrategy} to fix {@code save()} of entities with a composite/embedded
 * {@code @Id} on YDB.
 * <p>
 * Spring Data JDBC's generated {@code UPDATE} statement includes the composite id's own columns in the
 * {@code SET} clause (see {@link YdbColumns} for the root cause). YDB, unlike most relational databases, rejects
 * any {@code UPDATE ... SET pkColumn = ...} on compilation, regardless of the value - see
 * https://st.yandex-team.ru/YDBREQUESTS-8347 and the upstream report
 * https://github.com/spring-projects/spring-data-relational/issues/2338.
 * <p>
 * For entities with a simple (non-embedded) id, this class delegates to the default strategy unchanged - only
 * composite ids are affected by the bug.
 */
public class YdbDataAccessStrategy extends DelegatingDataAccessStrategy {

    private static final SqlIdentifier OLD_VERSION_PARAMETER = SqlIdentifier.unquoted("___ydb_oldOptimisticLockingVersion");

    private final NamedParameterJdbcOperations operations;
    private final JdbcConverter converter;
    private final RelationalMappingContext context;
    private final SqlRenderer sqlRenderer;
    private final Map<Class<?>, Optional<PreparedUpdate>> preparedUpdates = new ConcurrentHashMap<>();

    public YdbDataAccessStrategy(DataAccessStrategy delegate, NamedParameterJdbcOperations operations,
            JdbcConverter converter, Dialect dialect) {

        super(delegate);
        this.operations = operations;
        this.converter = converter;
        this.context = converter.getMappingContext();
        this.sqlRenderer = SqlRenderer.create(new RenderContextFactory(dialect).createRenderContext());
    }

    @Override
    public <S> boolean update(S instance, Class<S> domainType) {

        Optional<PreparedUpdate> prepared = prepare(domainType);
        if (prepared.isEmpty()) {
            return super.update(instance, domainType);
        }

        PreparedUpdate update = prepared.get();
        if (update.updateSql == null) {
            return true;
        }

        MapSqlParameterSource params = toParameters(instance, update.entity);
        return operations.update(update.updateSql, params) != 0;
    }

    @Override
    public <S> boolean updateWithVersion(S instance, Class<S> domainType, Number previousVersion) {

        Optional<PreparedUpdate> prepared = prepare(domainType);
        if (prepared.isEmpty()) {
            return super.updateWithVersion(instance, domainType, previousVersion);
        }

        PreparedUpdate update = prepared.get();
        if (update.updateWithVersionSql == null) {
            // no @Version property - behave like a plain update
            return update(instance, domainType);
        }

        MapSqlParameterSource params = toParameters(instance, update.entity);
        params.addValue(paramName(OLD_VERSION_PARAMETER), previousVersion);

        int affected = operations.update(update.updateWithVersionSql, params);
        if (affected == 0) {
            throw new OptimisticLockingFailureException(
                    "Optimistic lock exception on saving entity of type " + domainType.getName());
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private <S> Optional<PreparedUpdate> prepare(Class<S> domainType) {
        return preparedUpdates.computeIfAbsent(domainType, type -> {

            RelationalPersistentEntity<?> entity = context.getRequiredPersistentEntity(type);
            RelationalPersistentProperty idProperty = entity.getRequiredIdProperty();

            if (!idProperty.isEmbedded()) {
                // simple id - not affected by the bug, let the default strategy handle it
                return Optional.empty();
            }

            return Optional.of(buildPreparedUpdate(entity));
        });
    }

    private PreparedUpdate buildPreparedUpdate(RelationalPersistentEntity<?> entity) {

        Table table = Table.create(entity.getQualifiedTableName());
        YdbColumns columns = YdbColumns.resolve(entity, context, converter);

        if (columns.getUpdatableColumns().isEmpty() || columns.getIdColumns().isEmpty()) {
            return new PreparedUpdate(entity, null, null);
        }

        List<AssignValue> assignments = columns.getUpdatableColumns().stream()
                .map(column -> Assignments.value(table.column(column), SQL.bindMarker(":" + paramName(column))))
                .collect(Collectors.toList());

        Condition idCondition = null;
        for (SqlIdentifier idColumn : columns.getIdColumns()) {

            Condition equalsId = table.column(idColumn).isEqualTo(SQL.bindMarker(":" + paramName(idColumn)));
            idCondition = idCondition == null ? equalsId : idCondition.and(equalsId);
        }

        String updateSql = sqlRenderer.render(Update.builder().table(table).set(assignments).where(idCondition).build());

        String updateWithVersionSql = null;
        RelationalPersistentProperty versionProperty = entity.getVersionProperty();
        if (versionProperty != null) {

            Condition withOldVersion = idCondition.and(table.column(versionProperty.getColumnName())
                    .isEqualTo(SQL.bindMarker(":" + paramName(OLD_VERSION_PARAMETER))));

            updateWithVersionSql = sqlRenderer
                    .render(Update.builder().table(table).set(assignments).where(withOldVersion).build());
        }

        return new PreparedUpdate(entity, updateSql, updateWithVersionSql);
    }

    private MapSqlParameterSource toParameters(Object instance, RelationalPersistentEntity<?> entity) {

        MapSqlParameterSource params = new MapSqlParameterSource();
        collectValues(instance, entity, "", params);
        return params;
    }

    private void collectValues(Object instance, RelationalPersistentEntity<?> entity, String prefix,
            MapSqlParameterSource target) {

        PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(instance);

        entity.doWithAll(property -> {

            if (!property.isEntity()) {

                SqlIdentifier columnName = property.getColumnName().transform(prefix::concat);
                Object value = accessor.getProperty(property);

                Class<?> columnType = converter.getColumnType(property);
                SQLType sqlType = converter.getTargetSqlType(property);
                JdbcValue jdbcValue = converter.writeJdbcValue(value, columnType, sqlType);

                target.addValue(paramName(columnName), jdbcValue.getValue(), jdbcValue.getJdbcType().getVendorTypeNumber());
            } else if (property.isEmbedded()) {

                Object embeddedValue = accessor.getProperty(property);
                if (embeddedValue == null) {
                    return;
                }

                RelationalPersistentEntity<?> embeddedEntity = context
                        .getRequiredPersistentEntity(converter.getColumnType(property));

                collectValues(embeddedValue, embeddedEntity, prefix + property.getEmbeddedPrefix(), target);
            }
        });
    }

    private static String paramName(SqlIdentifier identifier) {
        return identifier.getReference().replaceAll("[^A-Za-z0-9_]", "_");
    }

    private static final class PreparedUpdate {

        private final RelationalPersistentEntity<?> entity;
        private final String updateSql;
        private final String updateWithVersionSql;

        private PreparedUpdate(RelationalPersistentEntity<?> entity, String updateSql, String updateWithVersionSql) {
            this.entity = entity;
            this.updateSql = updateSql;
            this.updateWithVersionSql = updateWithVersionSql;
        }
    }
}
