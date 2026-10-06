package tech.ydb.data.composite_key;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import tech.ydb.data.YdbBaseTest;
import tech.ydb.data.composite_key.entity.CompositeKeyEntity;
import tech.ydb.data.composite_key.entity.CompositeKeyId;
import tech.ydb.data.composite_key.repository.CompositeKeyRepository;

/**
 * Reproduces YDBREQUESTS-8347: {@code save()} of an existing entity with a composite (embedded) {@code @Id}
 * used to fail with "Cannot update primary key column", because Spring Data JDBC's generated {@code UPDATE}
 * included the id's own columns in {@code SET} (see https://github.com/spring-projects/spring-data-relational/issues/2338).
 * <p>
 * Verification reads the persisted row directly via {@link NamedParameterJdbcTemplate} rather than
 * {@code repository.findById(...)}, because hydrating an entity with a composite id back from a row currently
 * fails in Spring Data JDBC 4.0.3 with an unrelated bug ("Cannot obtain ColumnInfo for embedded path", thrown
 * from {@code DefaultAggregatePath}/{@code MappingRelationalConverter}, entirely outside of this module's
 * {@code DataAccessStrategy}) - out of scope for this fix, which is only about the {@code UPDATE} statement.
 */
public class CompositeKeyUpdateTest extends YdbBaseTest {

    @Autowired
    private CompositeKeyRepository repository;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void updateExistingEntityWithCompositeId() {

        CompositeKeyId id = new CompositeKeyId("workspace-1", "scope-1");

        CompositeKeyEntity inserted = repository.save(new CompositeKeyEntity(id, "ACTIVE"));
        Assertions.assertNotNull(inserted.getVersion(), "expected @Version to be populated after insert");
        Assertions.assertEquals("ACTIVE", currentStatus(id));

        // reuses the instance returned by the insert (with its @Version populated), so save() below
        // takes the "update an existing aggregate" path instead of inserting a new row
        inserted.setStatus("ARCHIVED");
        repository.save(inserted);

        Assertions.assertEquals("ARCHIVED", currentStatus(id));
    }

    private String currentStatus(CompositeKeyId id) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("workspaceId", id.getWorkspaceId())
                .addValue("scopeId", id.getScopeId());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select status from composite_key_entity where workspace_id = :workspaceId and scope_id = :scopeId",
                params);

        return (String) row.get("status");
    }
}
