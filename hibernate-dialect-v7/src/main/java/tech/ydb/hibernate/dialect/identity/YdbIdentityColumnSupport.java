package tech.ydb.hibernate.dialect.identity;

import java.sql.JDBCType;
import org.hibernate.MappingException;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
import org.hibernate.generator.EventType;
import org.hibernate.id.insert.GetGeneratedKeysDelegate;
import org.hibernate.persister.entity.EntityPersister;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TINYINT;

/**
 * @author Ainur Mukhtarov
 */
public class YdbIdentityColumnSupport extends IdentityColumnSupportImpl {

    public static final YdbIdentityColumnSupport INSTANCE = new YdbIdentityColumnSupport();

    @Override
    public boolean hasDataTypeInIdentityColumn() {
        return false;
    }

    @Override
    public String getIdentityColumnString(int type) throws MappingException {
        return switch (type) {
            case TINYINT, SMALLINT -> "SmallSerial";
            case INTEGER -> "Serial";
            case BIGINT -> "BigSerial";
            default -> throw new MappingException(
                    "Ydb does not support identity key generation for sqlType: " + JDBCType.valueOf(type));
        };
    }

    @Override
    public boolean supportsIdentityColumns() {
        return true;
    }

    @Override
    public GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(EntityPersister persister) {
        return new GetGeneratedKeysDelegate(persister, false, EventType.INSERT);
    }
}
