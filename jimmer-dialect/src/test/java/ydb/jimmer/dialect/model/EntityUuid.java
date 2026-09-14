package ydb.jimmer.dialect.model;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

import java.util.UUID;

@org.babyfish.jimmer.sql.Entity
@Table(name = "benchmark_items")
public interface EntityUuid {
    @Id
    @Column(name = "id")
    UUID getId();

    @Column(name = "value")
    String value();
}
