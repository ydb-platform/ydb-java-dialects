package ydb.jimmer.dialect.model.type.ydbUuid;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

import java.util.UUID;

@Entity
@Table(name = "ydb_uuid")
public interface YdbUuid {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    UUID value();
}
