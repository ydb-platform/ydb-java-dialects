package tech.ydb.data.all_types_table.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import tech.ydb.data.core.convert.annotation.YdbType;
import tech.ydb.table.values.PrimitiveType;

/**
 * @author Madiyar Nurgazin
 */
@AllArgsConstructor
@Data
@Table("all_types_table")
public class AllTypesEntity implements Persistable<Integer> {

    @Id
    private Integer id;
    private String textColumn;
    private boolean boolColumn;
    private byte tinyintColumn;
    private short smallintColumn;
    private long bigintColumn;
    private float floatColumn;
    private double doubleColumn;
    private BigDecimal decimalColumn;
    private byte[] binaryColumn;
    @YdbType(PrimitiveType.Date)
    private LocalDate dateColumn;
    @YdbType(PrimitiveType.Datetime)
    private LocalDateTime datetimeColumn;
    private Instant timestampColumn;
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

    @Override
    public boolean isNew() {
        return false;
    }
}
