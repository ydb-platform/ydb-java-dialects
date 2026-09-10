package ydb.jimmer.dialect.model.versioning;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.KeyUniqueConstraint;
import org.babyfish.jimmer.sql.Table;
import org.babyfish.jimmer.sql.Version;

@Entity
//@KeyUniqueConstraint
@Table(name = "version_table")
public interface BookStore {
    @Id
    @Column(name = "id")
    int id();

//    @Key
//    @Column(name = "name")
//    String name();

    @Version
    @Column(name = "value")
    int value();
}
