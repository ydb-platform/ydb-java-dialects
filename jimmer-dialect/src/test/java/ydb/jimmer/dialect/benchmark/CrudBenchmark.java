package ydb.jimmer.dialect.benchmark;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.table.TableClient;
import ydb.jimmer.dialect.YdbDialect;
import ydb.jimmer.dialect.YqlClientBuilder;
import ydb.jimmer.dialect.model.EntityUuid;
import ydb.jimmer.dialect.model.EntityUuidDraft;
import ydb.jimmer.dialect.model.EntityUuidTable;
import ydb.jimmer.dialect.transaction.YqlClient;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(1)
public class CrudBenchmark {
    private static final String YDB_IMAGE = "cr.yandex/yc/yandex-docker-local-ydb:latest";
    private static final int GRPC_PORT = 2136;
    private static final int MON_PORT = 8765;
    private static final String TABLE = "benchmark_items";
    private static final String DDL = """
            CREATE TABLE %s (
                id Uuid,
                value String,
                PRIMARY KEY (id)
            );""".formatted(TABLE);

    @Param({"10", "20", "50", "100", "200", "500", "1000", "10000", "100000"})
    public int rowCount;

    private FixedHostPortGenericContainer<?> ydbContainer;
    private GrpcTransport transport;
    private TableClient tableClient;
    private DataSource dataSource;

    private List<EntityUuid> newEntities;

    private static JSqlClient sqlClient;
    private static YqlClient yqlClient;

    @Setup(Level.Trial)
    public void startInfrastructure() throws Exception {
        ydbContainer = new FixedHostPortGenericContainer<>(YDB_IMAGE)
                .withFixedExposedPort(GRPC_PORT, GRPC_PORT)
                .withExposedPorts(MON_PORT)
                .withEnv("YDB_USE_IN_MEMORY_PDISKS", "true")
                .withCreateContainerCmdModifier(cmd -> cmd.withHostName("localhost"))
                .waitingFor(
                        Wait.forHttp("/")
                                .forPort(MON_PORT)
                                .forStatusCodeMatching(code -> code < 500)
                                .withStartupTimeout(Duration.ofMinutes(2))
                );
        ydbContainer.start();

        String endpoint = "grpc://localhost:" + GRPC_PORT;
        String database = "/local";

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("tech.ydb.jdbc.YdbDriver");
        ds.setUrl("jdbc:ydb:" + endpoint + database);
        dataSource = ds;
        sqlClient = JSqlClient.newBuilder()
                .setDialect(new YdbDialect())
                .setConnectionManager(ConnectionManager.simpleConnectionManager(dataSource))
                .build();
        yqlClient = YqlClientBuilder.getYqlClient(dataSource);

        transport = GrpcTransport.forEndpoint(endpoint, database).build();
        tableClient = TableClient.newClient(transport).build();

        executeSchemeQuery(DDL);
    }

    private void executeSchemeQuery(String yql) {
        try (var session = tableClient
                .createSession(Duration.ofSeconds(10))
                .join().getValue()) {
            session.executeSchemeQuery(yql)
                    .join().expectSuccess("DDL failed: " + yql);
        }
    }

    @Setup(Level.Invocation)
    public void insertSeedRows() {
        EntityUuidTable table = EntityUuidTable.$;

        yqlClient.createDelete(table).execute();

        List<EntityUuid> seedEntities = new ArrayList<>();
        newEntities = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            UUID id = UUID.randomUUID();
            seedEntities.add(EntityUuidDraft.$.produce(entity -> {
                entity.setId(id);
                entity.setValue("entity-" + id);
            }));

            UUID newId = UUID.randomUUID();
            newEntities.add(EntityUuidDraft.$.produce(entity -> {
                entity.setId(newId);
                entity.setValue("entity-" + newId);
            }));
        }

        yqlClient.getEntities()
                .saveEntitiesCommand(seedEntities)
                .execute();
    }

    @TearDown(Level.Trial)
    public void stopInfrastructure() throws Exception {
        if (dataSource instanceof AutoCloseable ac) ac.close();
        if (tableClient  != null) tableClient.close();
        if (transport    != null) transport.close();
        if (ydbContainer != null) ydbContainer.stop();
    }

    @Benchmark
    public void readYdb() {
        yqlClient.getEntities().findAll(EntityUuid.class);
    }

    @Benchmark
    public void readJimmer() {
        sqlClient.getEntities().findAll(EntityUuid.class);
    }

    @Benchmark
    public void saveYdb() {
        yqlClient.getEntities()
                .saveEntitiesCommand(newEntities)
                .execute();
    }

    @Benchmark
    public void saveJimmer() {
        sqlClient.getEntities()
                .saveEntitiesCommand(newEntities)
                .execute();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CrudBenchmark.class.getSimpleName())
                .resultFormat(ResultFormatType.JSON)
                .result("jmh-crud-results.json")
                .shouldFailOnError(true)
                .build();
        new Runner(opt).run();
    }
}
