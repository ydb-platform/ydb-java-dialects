package tech.ydb.hibernate.lock;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "lock_test_entity")
public class TestEntity {
    @Id
    private Long id;

    @Column(name = "string_value", nullable = false)
    private String stringValue;

    @Version
    @Column(name = "version")
    private int version;
}
