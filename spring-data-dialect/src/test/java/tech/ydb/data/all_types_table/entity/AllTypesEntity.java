package tech.ydb.data.all_types_table.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import tech.ydb.data.core.convert.YdbType;
import tech.ydb.table.values.PrimitiveType;

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
    private long bigintColumn;
    private float floatColumn;
    private double doubleColumn;
    private BigDecimal decimalColumn;
    private byte[] binaryColumn;
    private LocalDate dateColumn;
    private LocalDateTime datetimeColumn;
    private Instant timestampColumn;
    @LastModifiedDate
    private LocalDateTime modified;
    @YdbType(PrimitiveType.Json)
    private String jsonColumn;
    @YdbType(PrimitiveType.JsonDocument)
    private String jsonDocumentColumn;
    @YdbType(PrimitiveType.Uint8)
    private byte uint8Column;
    @YdbType(PrimitiveType.Uint16)
    private short uint16Column;
    @YdbType(PrimitiveType.Uint32)
    private int uint32Column;
    @YdbType(PrimitiveType.Uint64)
    private long uint64Column;

    public AllTypesEntity() {
    }
}
