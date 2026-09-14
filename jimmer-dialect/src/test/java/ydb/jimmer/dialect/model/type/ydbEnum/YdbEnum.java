package ydb.jimmer.dialect.model.type.ydbEnum;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "ydb_enum")
public interface YdbEnum {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    Value value();
}
