package ydb.jimmer.dialect.model.type.ydbInt8;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "ydb_byte_class")
public interface YdbByteClass {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    Byte value();
}
