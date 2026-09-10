package ydb.jimmer.dialect.model.type.ydbDouble;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "ydb_double_class")
public interface YdbDoubleClass {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    Double value();
}
