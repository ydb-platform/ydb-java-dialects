package ydb.jimmer.dialect.model.type.ydbFloat;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "ydb_float")
public interface YdbFloat {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    float value();
}
