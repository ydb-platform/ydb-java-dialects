package tech.ydb.data.all_types_table.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import tech.ydb.data.core.convert.YQLType;
import tech.ydb.data.core.convert.annotation.YdbType;

/**
 * @author Madiyar Nurgazin
 */
@Table("all_types_table")
public class AllTypesEntity {
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
    @YdbType(YQLType.Date)
    private LocalDate dateColumn;
    @YdbType(YQLType.Datetime)
    private LocalDateTime datetimeColumn;
    private Instant timestampColumn;
    @YdbType(YQLType.Json)
    private String jsonColumn;
    @YdbType(YQLType.JsonDocument)
    private String jsonDocumentColumn;
    @YdbType(YQLType.Uint8)
    private byte uint8Column;
    @YdbType(YQLType.Uint16)
    private short uint16Column;
    @YdbType(YQLType.Uint32)
    private int uint32Column;
    @YdbType(YQLType.Uint64)
    private long uint64Column;

    public AllTypesEntity() {
    }

    public AllTypesEntity(Integer id, String textColumn, boolean boolColumn, byte tinyintColumn, short smallintColumn,
            long bigintColumn, float floatColumn, double doubleColumn, BigDecimal decimalColumn, byte[] binaryColumn,
            LocalDate dateColumn, LocalDateTime datetimeColumn, Instant timestampColumn, String jsonColumn,
            String jsonDocumentColumn, byte uint8Column, short uint16Column, int uint32Column, long uint64Column) {
        this.id = id;
        this.textColumn = textColumn;
        this.boolColumn = boolColumn;
        this.tinyintColumn = tinyintColumn;
        this.smallintColumn = smallintColumn;
        this.bigintColumn = bigintColumn;
        this.floatColumn = floatColumn;
        this.doubleColumn = doubleColumn;
        this.decimalColumn = decimalColumn;
        this.binaryColumn = binaryColumn;
        this.dateColumn = dateColumn;
        this.datetimeColumn = datetimeColumn;
        this.timestampColumn = timestampColumn;
        this.jsonColumn = jsonColumn;
        this.jsonDocumentColumn = jsonDocumentColumn;
        this.uint8Column = uint8Column;
        this.uint16Column = uint16Column;
        this.uint32Column = uint32Column;
        this.uint64Column = uint64Column;
    }

    public Integer getId() {
        return id;
    }

    public String getTextColumn() {
        return textColumn;
    }

    public BigDecimal getDecimalColumn() {
        return decimalColumn;
    }

    public void setJsonColumn(String jsonColumn) {
        this.jsonColumn = jsonColumn;
    }

    public void setJsonDocumentColumn(String jsonDocumentColumn) {
        this.jsonDocumentColumn = jsonDocumentColumn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                textColumn,
                boolColumn,
                tinyintColumn,
                smallintColumn,
                bigintColumn,
                floatColumn,
                doubleColumn,
                decimalColumn,
                binaryColumn,
                dateColumn,
                datetimeColumn,
                timestampColumn,
                jsonColumn,
                jsonDocumentColumn,
                uint8Column,
                uint16Column,
                uint32Column,
                uint64Column
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        AllTypesEntity other = (AllTypesEntity) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(textColumn, other.textColumn)
                && boolColumn == other.boolColumn
                && tinyintColumn == other.tinyintColumn
                && smallintColumn == other.smallintColumn
                && bigintColumn == other.bigintColumn
                && floatColumn == other.floatColumn
                && doubleColumn == other.doubleColumn
                && Objects.equals(decimalColumn, other.decimalColumn)
                && Arrays.equals(binaryColumn, other.binaryColumn)
                && Objects.equals(dateColumn, other.dateColumn)
                && Objects.equals(datetimeColumn, other.datetimeColumn)
                && Objects.equals(timestampColumn, other.timestampColumn)
                && Objects.equals(jsonColumn, other.jsonColumn)
                && Objects.equals(jsonDocumentColumn, other.jsonDocumentColumn)
                && uint8Column == other.uint8Column
                && uint16Column == other.uint16Column
                && uint32Column == other.uint32Column
                && uint64Column == other.uint64Column;
    }


}
