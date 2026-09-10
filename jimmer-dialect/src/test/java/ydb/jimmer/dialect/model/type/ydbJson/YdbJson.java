package ydb.jimmer.dialect.model.type.ydbJson;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "ydb_json")
public interface YdbJson {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    Json value();
}
