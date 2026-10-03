package ydb.jimmer.dialect.model.type.ydbString;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "ydb_byte_array")
public interface YdbByteArray {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    byte[] value();
}
