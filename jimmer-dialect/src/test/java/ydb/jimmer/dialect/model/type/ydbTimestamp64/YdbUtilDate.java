package ydb.jimmer.dialect.model.type.ydbTimestamp64;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table(name = "ydb_util_date")
public interface YdbUtilDate {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    Date value();
}
