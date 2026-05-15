package ydb.jimmer.dialect.model.type.ydbInt64;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "ydb_long_class")
public interface YdbLongClass {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    Long value();
}
