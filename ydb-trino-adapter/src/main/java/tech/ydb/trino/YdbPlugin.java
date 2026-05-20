package tech.ydb.trino;

import io.trino.plugin.jdbc.JdbcPlugin;

public class YdbPlugin
        extends JdbcPlugin
{
    public YdbPlugin()
    {
        super("ydb", YdbClientModule::new);
    }
}
