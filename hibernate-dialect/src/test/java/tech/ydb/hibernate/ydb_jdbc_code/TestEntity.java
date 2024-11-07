package tech.ydb.hibernate.ydb_jdbc_code;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import tech.ydb.hibernate.dialect.code.YdbJdbcCode;

/**
 * @author Kirill Kurdyukov
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "hibernate_test")
public class TestEntity {

    @Id
    @JdbcTypeCode(YdbJdbcCode.UINT8)
    private int id;

    @Column
    private BigDecimal default_bigDecimal;

    @Column
    @JdbcTypeCode(YdbJdbcCode.DECIMAL_31_9)
    private BigDecimal bigDecimal31_9;

    @Column
    @JdbcTypeCode(YdbJdbcCode.DECIMAL_35_0)
    private BigDecimal bigDecimal35_0;

    @Column
    @JdbcTypeCode(YdbJdbcCode.DECIMAL_35_9)
    private BigDecimal bigDecimal35_9;
}
