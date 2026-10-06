package tech.ydb.data.composite_key.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Reproduces the entity shape from YDBREQUESTS-8347: a composite {@code @Id} together with a {@code @Version}
 * property, whose {@code save()} of an already-persisted instance used to fail on YDB with
 * "Cannot update primary key column".
 */
@Table("composite_key_entity")
public class CompositeKeyEntity {

    @Id
    private CompositeKeyId id;

    private String status;

    @Version
    private Long version;

    public CompositeKeyEntity() {
    }

    public CompositeKeyEntity(CompositeKeyId id, String status) {
        this.id = id;
        this.status = status;
    }

    public CompositeKeyId getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }
}
