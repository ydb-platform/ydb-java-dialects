package ydb.jimmer.dialect.model.type.ydbTimestamp64;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

import java.time.Instant;

@Entity
@Table(name = "ydb_instant")
public interface YdbInstant {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    Instant value();
}
