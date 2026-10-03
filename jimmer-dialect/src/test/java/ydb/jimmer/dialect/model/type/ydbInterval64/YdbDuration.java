package ydb.jimmer.dialect.model.type.ydbInterval64;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

import java.time.Duration;

@Entity
@Table(name = "ydb_duration")
public interface YdbDuration {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    Duration value();
}
