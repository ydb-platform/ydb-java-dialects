package ydb.jimmer.dialect.model.type.ydbDate32;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

import java.sql.Date;
import java.time.LocalDate;

@Entity
@Table(name = "ydb_date")
public interface YdbDate {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    Date value();
}
