/*
 * This file is generated by jOOQ.
 */
package jooq.generated.ydb.default_schema.tables;


import java.util.Collection;

import jooq.generated.ydb.default_schema.DefaultSchema;
import jooq.generated.ydb.default_schema.Keys;
import jooq.generated.ydb.default_schema.tables.records.HardTableRecord;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.JSON;
import org.jooq.JSONB;
import org.jooq.Name;
import org.jooq.PlainSQL;
import org.jooq.QueryPart;
import org.jooq.SQL;
import org.jooq.Schema;
import org.jooq.Select;
import org.jooq.Stringly;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import tech.ydb.jooq.binding.JsonBinding;
import tech.ydb.jooq.binding.JsonDocumentBinding;
import tech.ydb.jooq.binding.YsonBinding;
import tech.ydb.jooq.value.YSON;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class HardTable extends TableImpl<HardTableRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>DEFAULT_SCHEMA.hard_table</code>
     */
    public static final HardTable HARD_TABLE = new HardTable();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<HardTableRecord> getRecordType() {
        return HardTableRecord.class;
    }

    /**
     * The column <code>DEFAULT_SCHEMA.hard_table.id</code>.
     */
    public final TableField<HardTableRecord, byte[]> ID = createField(DSL.name("id"), SQLDataType.VARBINARY, this, "");

    /**
     * The column <code>DEFAULT_SCHEMA.hard_table.first</code>.
     */
    public final TableField<HardTableRecord, JSON> FIRST = createField(DSL.name("first"), SQLDataType.JSON, this, "", new JsonBinding());

    /**
     * The column <code>DEFAULT_SCHEMA.hard_table.second</code>.
     */
    public final TableField<HardTableRecord, JSONB> SECOND = createField(DSL.name("second"), SQLDataType.JSONB, this, "", new JsonDocumentBinding());

    /**
     * The column <code>DEFAULT_SCHEMA.hard_table.third</code>.
     */
    public final TableField<HardTableRecord, YSON> THIRD = createField(DSL.name("third"), SQLDataType.OTHER, this, "", new YsonBinding());

    private HardTable(Name alias, Table<HardTableRecord> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private HardTable(Name alias, Table<HardTableRecord> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>DEFAULT_SCHEMA.hard_table</code> table reference
     */
    public HardTable(String alias) {
        this(DSL.name(alias), HARD_TABLE);
    }

    /**
     * Create an aliased <code>DEFAULT_SCHEMA.hard_table</code> table reference
     */
    public HardTable(Name alias) {
        this(alias, HARD_TABLE);
    }

    /**
     * Create a <code>DEFAULT_SCHEMA.hard_table</code> table reference
     */
    public HardTable() {
        this(DSL.name("hard_table"), null);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : DefaultSchema.DEFAULT_SCHEMA;
    }

    @Override
    public UniqueKey<HardTableRecord> getPrimaryKey() {
        return Keys.PK_HARD_TABLE;
    }

    @Override
    public HardTable as(String alias) {
        return new HardTable(DSL.name(alias), this);
    }

    @Override
    public HardTable as(Name alias) {
        return new HardTable(alias, this);
    }

    @Override
    public HardTable as(Table<?> alias) {
        return new HardTable(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public HardTable rename(String name) {
        return new HardTable(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public HardTable rename(Name name) {
        return new HardTable(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public HardTable rename(Table<?> name) {
        return new HardTable(name.getQualifiedName(), null);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public HardTable where(Condition condition) {
        return new HardTable(getQualifiedName(), aliased() ? this : null, null, condition);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public HardTable where(Collection<? extends Condition> conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public HardTable where(Condition... conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public HardTable where(Field<Boolean> condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public HardTable where(SQL condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public HardTable where(@Stringly.SQL String condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public HardTable where(@Stringly.SQL String condition, Object... binds) {
        return where(DSL.condition(condition, binds));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public HardTable where(@Stringly.SQL String condition, QueryPart... parts) {
        return where(DSL.condition(condition, parts));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public HardTable whereExists(Select<?> select) {
        return where(DSL.exists(select));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public HardTable whereNotExists(Select<?> select) {
        return where(DSL.notExists(select));
    }
}
