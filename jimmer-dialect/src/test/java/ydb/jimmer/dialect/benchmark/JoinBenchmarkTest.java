package ydb.jimmer.dialect.benchmark;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.JoinType;
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
import ydb.jimmer.dialect.model.Group;
import ydb.jimmer.dialect.model.GroupDraft;
import ydb.jimmer.dialect.model.Student;
import ydb.jimmer.dialect.model.StudentDraft;
import ydb.jimmer.dialect.model.StudentTable;
import ydb.jimmer.dialect.transaction.YqlClient;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(0)
public class JoinBenchmarkTest extends AbstractSelectTest {
    private static final int size = 1000;

    private static final List<Student> students;
    private static final List<Group> groups;

    static {
        students = new ArrayList<>();
        groups = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            UUID value = UUID.randomUUID();
            Group group = GroupDraft.$.produce(g -> {
                g.setId(value);
                g.setName("name" + value);
            });
            groups.add(group);
            students.add(StudentDraft.$.produce(entity -> {
                entity.setId(UUID.randomUUID());
                entity.setName("name1" + value);
                entity.setGroup(group);
            }));
            students.add(StudentDraft.$.produce(entity -> {
                entity.setId(UUID.randomUUID());
                entity.setName("name2" + value);
                entity.setGroup(group);
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
        initDatabase();

        yqlClient.getEntities()
                .saveEntitiesCommand(students)
                .execute();

        yqlClient.getEntities()
                .saveEntitiesCommand(groups)
                .execute();
    }

    @Benchmark
    public void benchmarkLeft() {
        StudentTable table = StudentTable.$;
        getYqlClient()
                .createQuery(table)
                .orderBy(table.group(JoinType.LEFT).name().asc())
                .select(table);
    }

    @Test
    void runBenchmarks() throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(JoinBenchmarkTest.class.getSimpleName())
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
