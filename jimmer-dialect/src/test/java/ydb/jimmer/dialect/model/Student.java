package ydb.jimmer.dialect.model;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.ForeignKeyType;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.JoinColumn;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.Table;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import javax.annotation.Nullable;
import java.util.UUID;

@Entity
@Table(name = "student")
public interface Student {
    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    @Column(name = "id")
    UUID id();

    @Column(name = "name")
    String name();

    @ManyToOne
    @Nullable
    @JoinColumn(name = "group", foreignKeyType = ForeignKeyType.FAKE)
    Group group();
}
