package tech.ydb.data.all_types_table.entity;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Madiyar Nurgazin
 */
@AllArgsConstructor
@Data
@Table("all_types_table")
public class AllTypesEntity {
    @Id
    private int id;
    private String textColumn;
    private boolean boolColumn;
    private byte tinyintColumn;
    private short smallintColumn;
    private BigInteger bigintColumn;
    private float floatColumn;
    private double doubleColumn;
    private BigDecimal decimalColumn;
    private byte[] binaryColumn;
    private LocalDate dateColumn;
    private LocalDateTime datetimeColumn;
    private Instant timestampColumn;
    @CreatedDate
    private LocalDateTime created;

    public AllTypesEntity() {
    }
}
