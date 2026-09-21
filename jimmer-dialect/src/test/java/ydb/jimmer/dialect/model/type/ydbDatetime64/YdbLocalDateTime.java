package ydb.jimmer.dialect.model.type.ydbDatetime64;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ydb_local_date_time")
public interface YdbLocalDateTime {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    LocalDateTime value();
}
