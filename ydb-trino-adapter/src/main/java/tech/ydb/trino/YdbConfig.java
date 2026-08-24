package tech.ydb.trino;

import io.airlift.configuration.Config;
import io.airlift.configuration.ConfigDescription;
import io.airlift.units.DataSize;
import io.airlift.units.MinDataSize;

import static io.airlift.units.DataSize.Unit.MEGABYTE;

public class YdbConfig
{
    private DataSize mergeMaxBufferSize = DataSize.of(64, MEGABYTE);

    @MinDataSize("1B")
    public DataSize getMergeMaxBufferSize()
    {
        return mergeMaxBufferSize;
    }

    @Config("merge.max-buffer-size")
    @ConfigDescription("Maximum retained memory for a single atomic YDB MERGE sink")
    public YdbConfig setMergeMaxBufferSize(DataSize mergeMaxBufferSize)
    {
        this.mergeMaxBufferSize = mergeMaxBufferSize;
        return this;
    }
}
