package ydb.jimmer.dialect.model.type.ydbInt16;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "ydb_short")
public interface YdbShort {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    short value();
}
