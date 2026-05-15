package ydb.jimmer.dialect.model.type.ydbBool;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "ydb_boolean")
public interface YdbBoolean {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    boolean value();
}
