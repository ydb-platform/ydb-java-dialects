package ydb.jimmer.dialect.benchmark;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.runner.format.OutputFormatFactory;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import ydb.jimmer.dialect.AbstractSelectTest;
import ydb.jimmer.dialect.YdbDialect;
import ydb.jimmer.dialect.YqlClientBuilder;
import ydb.jimmer.dialect.model.Entity;
import ydb.jimmer.dialect.model.EntityDraft;
import ydb.jimmer.dialect.model.EntityTable;
import ydb.jimmer.dialect.transaction.YqlClient;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(0)
public class CrudScanBenchmarkTest extends AbstractSelectTest {
    private static final String TABLE_NAME = "simple_table";
    private static final String TYPE_NAME = "Int32";

    private static final int size = 1000;

    private static final String[] values;
    private static final List<Entity> existingEntities;
    private static final List<Integer> existingIds;
    private static final List<Entity> newEntities;

    static {
        values = new String[size];
        existingEntities = new ArrayList<>();
        existingIds = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int value = i;
            values[i] = String.valueOf(value);
            existingEntities.add(EntityDraft.$.produce(entity -> {
                entity.setId(value);
                entity.setValue(value);
            }));
            existingIds.add(value);
        }

        newEntities = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int value = size + i;
            newEntities.add(EntityDraft.$.produce(entity -> {
                entity.setId(value);
                entity.setValue(value);
            }));
        }
    }

    protected static JSqlClient sqlClient;
    protected static YqlClient yqlClient;

    static {
        DataSource dataSource = new DriverManagerDataSource(getJdbcURL());
        sqlClient = JSqlClient.newBuilder()
                .setDialect(new YdbDialect())
                .setConnectionManager(ConnectionManager.simpleConnectionManager(dataSource))
                .build();
        yqlClient = YqlClientBuilder.getYqlClient(dataSource);
    }

    @Setup(Level.Invocation)
    public void setup() {
        createTable(TABLE_NAME, TYPE_NAME);

        yqlClient.getEntities()
                .saveEntitiesCommand(existingEntities)
                .execute();
    }

    @TearDown(Level.Invocation)
    public void tearDown() {
        dropTable(TABLE_NAME);
    }

    @Benchmark
    public void benchmarkRead() {
        yqlClient.snapshotReadOnly(() -> yqlClient.getEntities().findAll(Entity.class));
    }

    @Benchmark
    public void benchmarkScan() {
        EntityTable table = EntityTable.$;
        yqlClient.createQuery(table)
                .where(table.value().ge(size/2))
                .where(table.value().le(size))
                .select(table);
    }

    @Benchmark
    public void benchmarkScanDialectOnly() {
        EntityTable table = EntityTable.$;
        sqlClient.createQuery(table)
                .where(table.value().ge(size/2))
                .where(table.value().le(size))
                .select(table);
    }

    @Benchmark
    public void benchmarkReadDialectOnly() {
        sqlClient.getEntities().findAll(Entity.class);
    }

    @Benchmark
    public void benchmarkSave() {
        yqlClient.getEntities()
                .saveEntitiesCommand(newEntities)
                .execute();
    }

    @Benchmark
    public void benchmarkSaveDialectOnly() {
        sqlClient.getEntities()
                .saveEntitiesCommand(newEntities)
                .execute();
    }

    @Benchmark
    public void benchmarkUpdate() {
        yqlClient.getEntities()
                .saveEntitiesCommand(existingEntities)
                .setMode(SaveMode.UPDATE_ONLY)
                .execute();
    }

    @Benchmark
    public void benchmarkDelete() {
        yqlClient.getEntities().deleteAll(Entity.class, existingIds);
    }

    @Benchmark
    public void benchmarkDeleteDialectOnly() {
        sqlClient.getEntities().deleteAll(Entity.class, existingIds);
    }

    @Test
    void runBenchmarks() throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(CrudScanBenchmarkTest.class.getSimpleName())
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();

        OutputFormat out = OutputFormatFactory.createFormatInstance(
                System.out,
                VerboseMode.NORMAL
        );


        new Runner(opts, out).run();
    }
}
