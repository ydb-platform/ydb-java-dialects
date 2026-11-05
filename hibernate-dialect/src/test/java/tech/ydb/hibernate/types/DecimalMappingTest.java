package tech.ydb.hibernate.types;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.concurrent.atomic.AtomicReference;
import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.mapping.BasicValue;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tech.ydb.hibernate.dialect.YdbDialect;

public class DecimalMappingTest {
    private static StandardServiceRegistry registry;
    private static Metadata metadata;
    private static YdbDialect dialect;

    @BeforeAll
    static void setUp() {
        DialectResolutionInfo info = (DialectResolutionInfo) Proxy.newProxyInstance(
                DecimalMappingTest.class.getClassLoader(), new Class<?>[]{DialectResolutionInfo.class},
                (proxy, method, args) -> {
                    if (method.getReturnType() == int.class) {
                        return 1;
                    }
                    return method.getReturnType() == String.class ? "YDB" : null;
                });
        dialect = new YdbDialect(info);
        registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", dialect)
                .applySetting("hibernate.temp.use_jdbc_metadata_defaults", false)
                .build();
        metadata = new MetadataSources(registry).addAnnotatedClass(DecimalColumns.class).buildMetadata();
    }

    @AfterAll
    static void tearDown() {
        StandardServiceRegistryBuilder.destroy(registry);
    }

    @Test
    void mapsColumnPrecisionAndScale() throws Exception {
        assertMapping("amount", 18, 2, Types.NUMERIC);
        assertMapping("largeAmount", 35, 10, Types.NUMERIC);
        assertMapping("fraction", 35, 35, Types.NUMERIC);
        assertMapping("integerAmount", 35, 0, Types.NUMERIC);
        assertMapping("decimal22_9", 22, 9, Types.NUMERIC);
        assertMapping("decimal31_9", 31, 9, Types.NUMERIC);
        assertMapping("decimal35_0", 35, 0, Types.NUMERIC);
        assertMapping("decimal35_9", 35, 9, Types.NUMERIC);
    }

    @Test
    void preservesDefaults() throws Exception {
        assertMapping("defaultDecimal", 22, 9, Types.NUMERIC);
        assertMapping("defaultInteger", 22, 0, Types.NUMERIC);
    }

    @Test
    void rejectsInvalidColumnParameters() {
        assertThrows(MappingException.class,
                () -> new MetadataSources(registry).addAnnotatedClass(InvalidPrecision.class).buildMetadata());
        assertThrows(MappingException.class,
                () -> new MetadataSources(registry).addAnnotatedClass(InvalidScale.class).buildMetadata());
    }

    @Test
    void resolvesNativeResultMetadata() {
        for (int jdbcCode : new int[]{Types.NUMERIC, Types.DECIMAL}) {
            JdbcType type = dialect.resolveSqlTypeDescriptor("Decimal(35, 10)", jdbcCode, 35, 10,
                    ((MetadataImplementor) metadata).getTypeConfiguration().getJdbcTypeRegistry());
            assertEquals(18634, type.getJdbcTypeCode());
            assertEquals(Types.DECIMAL, type.getDefaultSqlTypeCode());
            assertEquals(Types.DECIMAL, type.getDdlTypeCode());
        }
    }

    private static void assertMapping(String field, int precision, int scale, int ddlCode) throws Exception {
        BasicValue value = (BasicValue) metadata.getEntityBinding(DecimalColumns.class.getName())
                .getProperty(field).getValue();
        org.hibernate.mapping.Column column = (org.hibernate.mapping.Column) value.getSelectables().get(0);
        assertEquals("Decimal(" + precision + ", " + scale + ")", column.getSqlType(metadata));
        JdbcType jdbcType = value.resolve().getJdbcType();
        assertEquals(ddlCode, jdbcType.getDefaultSqlTypeCode());
        assertEquals(ddlCode, jdbcType.getDdlTypeCode());
        int decimalCode = jdbcType.getJdbcTypeCode() - (1 << 14);
        assertEquals(precision, decimalCode >> 6);
        assertEquals(scale, decimalCode & 63);
        assertBinding(value.resolve(), BigDecimal.ZERO, jdbcType.getJdbcTypeCode());
        assertBinding(value.resolve(), null, jdbcType.getJdbcTypeCode());
    }

    private static <T> void assertBinding(BasicValue.Resolution<T> resolution, BigDecimal value, int jdbcCode)
            throws Exception {
        AtomicReference<Object[]> bound = new AtomicReference<>();
        PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                DecimalMappingTest.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setObject")) {
                        bound.set(args);
                    }
                    return null;
                });
        var javaType = resolution.getDomainJavaType();
        resolution.getJdbcType().getBinder(javaType).bind(statement, javaType.wrap(value, null), 1, null);
        assertEquals(1, bound.get()[0]);
        assertEquals(value, bound.get()[1]);
        assertEquals(jdbcCode, bound.get()[2]);
    }

    @Entity
    public static class DecimalColumns {
        @Id
        private long id;
        @Column(precision = 18, scale = 2)
        private BigDecimal amount;
        @Column(precision = 35, scale = 10)
        private BigDecimal largeAmount;
        @Column(precision = 35, scale = 35)
        private BigDecimal fraction;
        @Column(precision = 35, scale = 0)
        private BigDecimal integerAmount;
        private BigDecimal defaultDecimal;
        private BigInteger defaultInteger;
        @Column(precision = 22, scale = 9)
        private BigDecimal decimal22_9;
        @Column(precision = 31, scale = 9)
        private BigDecimal decimal31_9;
        @Column(precision = 35, scale = 0)
        private BigDecimal decimal35_0;
        @Column(precision = 35, scale = 9)
        private BigDecimal decimal35_9;
    }

    @Entity
    public static class InvalidPrecision {
        @Id
        private long id;
        @Column(precision = 36, scale = 0)
        private BigDecimal amount;
    }

    @Entity
    public static class InvalidScale {
        @Id
        private long id;
        @Column(precision = 18, scale = 19)
        private BigDecimal amount;
    }
}
