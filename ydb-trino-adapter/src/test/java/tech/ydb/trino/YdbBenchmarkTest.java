package tech.ydb.trino;

import io.trino.Session;
import io.trino.testing.QueryRunner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.trino.testing.TestingSession.testSessionBuilder;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class YdbBenchmarkTest
{
    private static final int MIN_POWER = 20;   // 2^1 = 2
    private static final int MAX_POWER = 23;  // 2^22 = 4,194,304
    private static final int WARMUP_RUNS = 2;
    private static final int MEASURED_RUNS = 5;

    private static final String TABLE_NAME = "bench_table";
    private static final String SELECT_SQL = "SELECT count(*) FROM " + TABLE_NAME + " AS a JOIN " + TABLE_NAME + " AS b ON a.id = b.id * 2";

    // Local YDB configuration
    private static final String YDB_ENDPOINT = "localhost:2136";
    private static final String YDB_DATABASE = "/local";
    private static final String YDB_USE_TLS = "false";
    private static final String YDB_AUTH_TOKEN = null;

    private QueryRunner queryRunner;
    private Session session;
    private String jdbcUrl;

    @BeforeAll
    void setUp() throws Exception
    {
        jdbcUrl = buildJdbcUrl();
        queryRunner = YdbQueryRunner.builder().build();
        session = testSessionBuilder()
                .setCatalog("ydb")
                .setSchema(YdbQueryRunner.TPCH_SCHEMA)
                .build();
    }

    @AfterAll
    void tearDown()
    {
        if (queryRunner != null) {
            try {
                queryRunner.close();
            }
            catch (Exception ignored) {
            }
        }
    }

    private String buildJdbcUrl()
    {
        StringBuilder url = new StringBuilder("jdbc:ydb:");
        url.append("grpc://");
        url.append(YDB_ENDPOINT);
        url.append(YDB_DATABASE);
        url.append("?useQueryService=true");
        url.append("&sessionPoolMaxSize=10");
        url.append("&sessionPoolMinSize=3");
        url.append("&grpcKeepAliveTime=30000");
        url.append("&grpcKeepAliveTimeout=10000");
        return url.toString();
    }

    @Test
    public void benchmarkScan() throws Exception
    {
        List<BenchmarkResult> results = new ArrayList<>();

        for (int power = MIN_POWER; power <= MAX_POWER; power++) {
            int rowCount = 1 << power;  // 2^power
            System.out.println("\n========== Testing 2^" + power + " = " + rowCount + " rows ==========");

            // Prepare table
            prepareTable(rowCount);

            // Run benchmark
            BenchmarkResult result = runBenchmark(rowCount, power);
            results.add(result);

            // Print detailed stats for this power immediately
            System.out.println("\n--- Detailed stats for 2^" + power + " = " + rowCount + " rows ---");
            System.out.println("Trino: mean=" + String.format("%.2f", result.trinoMean) +
                    "ms, median=" + String.format("%.2f", result.trinoMedian) +
                    "ms, std=" + String.format("%.2f", result.trinoStdDev) +
                    "ms, min=" + result.trinoMin + "ms, max=" + result.trinoMax + "ms");
            System.out.println("JDBC:  mean=" + String.format("%.2f", result.jdbcMean) +
                    "ms, median=" + String.format("%.2f", result.jdbcMedian) +
                    "ms, std=" + String.format("%.2f", result.jdbcStdDev) +
                    "ms, min=" + result.jdbcMin + "ms, max=" + result.jdbcMax + "ms");
            System.out.println("Ratio (Trino/JDBC): " + String.format("%.2f", result.trinoMean / result.jdbcMean) + "x");
            System.out.println();
        }

        // Print aggregated results table at the end
        printAggregatedResults(results);
    }

    /**
     * Rows generated per UPSERT when batching. 2^16 = 65536 rows ≈ 1MB, comfortably
     * under YDB's 64MB per-query buffer (#2029 Out of buffer memory). Powers up to
     * BATCH_POWER use a single CROSS-JOIN UPSERT; larger powers reuse this generator
     * with a per-batch id offset.
     */
    private static final int BATCH_POWER = 16;
    private static final int BATCH_SIZE = 1 << BATCH_POWER;

    private void prepareTable(int rowCount) throws Exception
    {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS " + TABLE_NAME);
            statement.execute("DROP TABLE IF EXISTS temp_bench_2");

            // Create base table with 2 rows (0 and 1)
            statement.execute("CREATE TABLE temp_bench_2 (n Int32 NOT NULL, PRIMARY KEY (n))");
            statement.execute("UPSERT INTO temp_bench_2 (n) VALUES (0), (1)");

            // Create target table
            statement.execute("CREATE TABLE " + TABLE_NAME + " (id Uint64 NOT NULL, name Utf8, PRIMARY KEY (id))");

            int power = Integer.numberOfTrailingZeros(Integer.highestOneBit(rowCount));
            if (power <= BATCH_POWER) {
                // Single UPSERT — fits in YDB's query buffer.
                statement.execute(buildBinaryCrossJoinInsert(power));
            }
            else {
                // Batched UPSERTs: reuse the 2^BATCH_POWER-row generator with a per-batch id offset,
                // so each query materializes only BATCH_SIZE rows.
                int batches = rowCount >>> BATCH_POWER;
                for (int b = 0; b < batches; b++) {
                    long offset = (long) b * BATCH_SIZE;
                    statement.execute(buildBatchedCrossJoinInsert(offset));
                }
            }

            // Verify row count
            try (ResultSet rs = statement.executeQuery("SELECT count(*) FROM " + TABLE_NAME)) {
                rs.next();
                long actualCount = rs.getLong(1);
                if (actualCount != rowCount) {
                    System.out.println("WARNING: Expected " + rowCount + " rows, got " + actualCount);
                }
            }

            // Cleanup temp table
            statement.execute("DROP TABLE temp_bench_2");
        }
    }

    private String buildBinaryCrossJoinInsert(int tableCount)
    {
        StringBuilder sql = new StringBuilder();
        sql.append("UPSERT INTO ").append(TABLE_NAME).append(" (id, name) ");

        if (tableCount == 1) {
            // 2 rows - simple select
            sql.append("SELECT n, CAST(n AS Utf8) FROM temp_bench_2");
        } else {
            // Use ROW_NUMBER() to generate sequential IDs from 0 to 2^tableCount - 1
            sql.append("SELECT CAST(ROW_NUMBER() OVER() - 1 AS Uint64) AS id, ");
            sql.append("CAST(ROW_NUMBER() OVER() - 1 AS Utf8) AS name ");

            // Build FROM with cross joins
            sql.append("FROM temp_bench_2 AS a");
            for (int i = 1; i < tableCount; i++) {
                char alias = (char) ('a' + i);
                sql.append(" CROSS JOIN temp_bench_2 AS ").append(alias);
            }
        }
        return sql.toString();
    }

    /**
     * Batched variant: produces ids {@code offset .. offset + BATCH_SIZE - 1} by reusing the
     * 2^BATCH_POWER-row CROSS-JOIN generator and adding {@code offset} to ROW_NUMBER.
     */
    private String buildBatchedCrossJoinInsert(long offset)
    {
        StringBuilder sql = new StringBuilder();
        sql.append("UPSERT INTO ").append(TABLE_NAME).append(" (id, name) ");
        sql.append("SELECT CAST(ROW_NUMBER() OVER() - 1 + ").append(offset).append(" AS Uint64) AS id, ");
        sql.append("CAST(ROW_NUMBER() OVER() - 1 + ").append(offset).append(" AS Utf8) AS name ");
        sql.append("FROM temp_bench_2 AS a");
        for (int i = 1; i < BATCH_POWER; i++) {
            char alias = (char) ('a' + i);
            sql.append(" CROSS JOIN temp_bench_2 AS ").append(alias);
        }
        return sql.toString();
    }

    private BenchmarkResult runBenchmark(int rowCount, int power) throws Exception
    {
        // Warmup
        for (int i = 0; i < WARMUP_RUNS; i++) {
            queryRunner.execute(session, SELECT_SQL);
            try (Connection conn = DriverManager.getConnection(jdbcUrl);
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(SELECT_SQL)) {
                rs.next();
            }
        }

        // Measure Trino
        long[] trinoTimes = new long[MEASURED_RUNS];
        for (int i = 0; i < MEASURED_RUNS; i++) {
            long start = System.nanoTime();
            queryRunner.execute(session, SELECT_SQL);
            trinoTimes[i] = (System.nanoTime() - start) / 1_000_000;
        }

        // Measure JDBC
        long[] jdbcTimes = new long[MEASURED_RUNS];
        for (int i = 0; i < MEASURED_RUNS; i++) {
            try (Connection conn = DriverManager.getConnection(jdbcUrl);
                    Statement stmt = conn.createStatement()) {
                long start = System.nanoTime();
                try (ResultSet rs = stmt.executeQuery(SELECT_SQL)) {
                    rs.next();
                }
                jdbcTimes[i] = (System.nanoTime() - start) / 1_000_000;
            }
        }

        Stats trinoStats = new Stats(trinoTimes);
        Stats jdbcStats = new Stats(jdbcTimes);

        return new BenchmarkResult(
                power,
                rowCount,
                trinoStats.mean,
                trinoStats.median,
                trinoStats.stdDev,
                trinoStats.min,
                trinoStats.max,
                jdbcStats.mean,
                jdbcStats.median,
                jdbcStats.stdDev,
                jdbcStats.min,
                jdbcStats.max
        );
    }

    private void printAggregatedResults(List<BenchmarkResult> results)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n");
        sb.append("================================================================================\n");
        sb.append("                    YDB BENCHMARK AGGREGATED RESULTS\n");
        sb.append("================================================================================\n");
        sb.append(String.format("%-10s %-10s %-12s %-12s %-12s %-12s %-10s%n",
                "Rows", "Power", "TrinoMean", "TrinoMed", "JDBCMean", "JDBCMed", "Ratio"));
        sb.append("--------------------------------------------------------------------------------\n");

        for (BenchmarkResult r : results) {
            sb.append(String.format("%-10d %-10s %-12.2f %-12.2f %-12.2f %-12.2f %-10.2f%n",
                    r.rowCount,
                    "2^" + r.power,
                    r.trinoMean,
                    r.trinoMedian,
                    r.jdbcMean,
                    r.jdbcMedian,
                    r.trinoMean / r.jdbcMean
            ));
        }

        sb.append("================================================================================\n");
        System.out.println(sb.toString());
        System.out.flush();
    }

    private static class BenchmarkResult
    {
        final int power;
        final int rowCount;
        final double trinoMean, trinoMedian, trinoStdDev;
        final long trinoMin, trinoMax;
        final double jdbcMean, jdbcMedian, jdbcStdDev;
        final long jdbcMin, jdbcMax;

        BenchmarkResult(int power, int rowCount,
                double trinoMean, double trinoMedian, double trinoStdDev, long trinoMin, long trinoMax,
                double jdbcMean, double jdbcMedian, double jdbcStdDev, long jdbcMin, long jdbcMax)
        {
            this.power = power;
            this.rowCount = rowCount;
            this.trinoMean = trinoMean;
            this.trinoMedian = trinoMedian;
            this.trinoStdDev = trinoStdDev;
            this.trinoMin = trinoMin;
            this.trinoMax = trinoMax;
            this.jdbcMean = jdbcMean;
            this.jdbcMedian = jdbcMedian;
            this.jdbcStdDev = jdbcStdDev;
            this.jdbcMin = jdbcMin;
            this.jdbcMax = jdbcMax;
        }
    }

    private static class Stats
    {
        final double mean;
        final double median;
        final double stdDev;
        final long min;
        final long max;

        Stats(long[] times)
        {
            int n = times.length;
            long sum = 0;
            long lo = Long.MAX_VALUE;
            long hi = Long.MIN_VALUE;
            for (long t : times) {
                sum += t;
                if (t < lo) lo = t;
                if (t > hi) hi = t;
            }
            this.min = lo;
            this.max = hi;
            this.mean = (double) sum / n;

            long[] sorted = times.clone();
            Arrays.sort(sorted);
            this.median = (n % 2 == 0)
                    ? (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
                    : sorted[n / 2];

            double variance = 0;
            for (long t : times) {
                double diff = t - this.mean;
                variance += diff * diff;
            }
            variance /= (n - 1);
            this.stdDev = Math.sqrt(variance);
        }
    }
}
