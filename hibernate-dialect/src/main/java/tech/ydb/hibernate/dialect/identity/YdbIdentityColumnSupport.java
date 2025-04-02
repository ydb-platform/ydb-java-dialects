package tech.ydb.hibernate.dialect.identity;

import java.sql.JDBCType;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.insert.GetGeneratedKeysDelegate;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TINYINT;

/**
 * @author Kirill Kurdyukov
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
    public GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(
            PostInsertIdentityPersister persister,
            Dialect dialect
    ) {
        return new GetGeneratedKeysDelegate(persister, dialect, false);
    }
}
