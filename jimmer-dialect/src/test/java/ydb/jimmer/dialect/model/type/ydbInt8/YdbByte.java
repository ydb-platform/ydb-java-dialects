package ydb.jimmer.dialect.model.type.ydbInt8;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "ydb_byte")
public interface YdbByte {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    byte value();
}
