package ydb.jimmer.dialect.model;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import java.util.UUID;

@Entity
@Table(name = "group")
public interface Group {
    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    @Column(name = "id")
    UUID id();

    @Column(name = "name")
    String name();
}
