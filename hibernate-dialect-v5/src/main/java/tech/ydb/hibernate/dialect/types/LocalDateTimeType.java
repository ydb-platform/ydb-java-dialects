package tech.ydb.hibernate.dialect.types;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.LiteralType;
import org.hibernate.type.VersionType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaDescriptor;

public class LocalDateTimeType extends AbstractSingleColumnStandardBasicType<LocalDateTime>
        implements VersionType<LocalDateTime>, LiteralType<LocalDateTime> {

    public static final LocalDateTimeType INSTANCE = new LocalDateTimeType();

    public static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S", Locale.ENGLISH);

    public LocalDateTimeType() {
        super(LocalDateTimeTypeDescriptor.INSTANCE, LocalDateTimeJavaDescriptor.INSTANCE);
    }

    @Override
    public String getName() {
        return LocalDateTime.class.getSimpleName();
    }

    @Override
    protected boolean registerUnderJavaType() {
        return true;
    }

    @Override
    public String objectToSQLString(LocalDateTime value, Dialect dialect) {
        return "{ts '" + FORMATTER.format(value) + "'}";
    }

    @Override
    public LocalDateTime seed(SharedSessionContractImplementor session) {
        return LocalDateTime.now();
    }

    @Override
    public LocalDateTime next(LocalDateTime current, SharedSessionContractImplementor session) {
        return LocalDateTime.now();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Comparator<LocalDateTime> getComparator() {
        return ComparableComparator.INSTANCE;
    }
}