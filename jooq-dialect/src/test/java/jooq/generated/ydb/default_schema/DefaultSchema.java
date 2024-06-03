/*
 * This file is generated by jOOQ.
 */
package jooq.generated.ydb.default_schema;


import java.util.Arrays;
import java.util.List;

import jooq.generated.ydb.DefaultCatalog;
import jooq.generated.ydb.default_schema.tables.Episodes;
import jooq.generated.ydb.default_schema.tables.HardTable;
import jooq.generated.ydb.default_schema.tables.Seasons;
import jooq.generated.ydb.default_schema.tables.Series;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DefaultSchema extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>DEFAULT_SCHEMA</code>
     */
    public static final DefaultSchema DEFAULT_SCHEMA = new DefaultSchema();

    /**
     * The table <code>DEFAULT_SCHEMA.episodes</code>.
     */
    public final Episodes EPISODES = Episodes.EPISODES;

    /**
     * The table <code>DEFAULT_SCHEMA.hard_table</code>.
     */
    public final HardTable HARD_TABLE = HardTable.HARD_TABLE;

    /**
     * The table <code>DEFAULT_SCHEMA.seasons</code>.
     */
    public final Seasons SEASONS = Seasons.SEASONS;

    /**
     * The table <code>DEFAULT_SCHEMA.series</code>.
     */
    public final Series SERIES = Series.SERIES;

    /**
     * No further instances allowed
     */
    private DefaultSchema() {
        super("DEFAULT_SCHEMA", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            Episodes.EPISODES,
            HardTable.HARD_TABLE,
            Seasons.SEASONS,
            Series.SERIES
        );
    }
}
