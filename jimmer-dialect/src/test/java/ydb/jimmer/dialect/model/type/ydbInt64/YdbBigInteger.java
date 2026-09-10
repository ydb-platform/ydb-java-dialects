package ydb.jimmer.dialect.model.type.ydbInt64;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

import java.math.BigInteger;

@Entity
@Table(name = "ydb_big_integer")
public interface YdbBigInteger {
    @Id
    @Column(name = "id")
    int getId();

    @Column(name = "value")
    BigInteger value();
}
