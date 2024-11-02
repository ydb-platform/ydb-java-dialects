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

    @Column(precision = 3)
    private BigDecimal bigDecimal3_0;

    @Column(precision = 35, columnDefinition = "DECIMAL (35, 0)")
    private BigDecimal bigDecimal35_0;
}
