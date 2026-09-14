package ydb.jimmer.dialect.model.type.ydbInt32;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

import java.sql.Time;
import java.time.LocalTime;

@Entity
@Table(name = "ydb_time")
public interface YdbTime {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    Time value();
}
