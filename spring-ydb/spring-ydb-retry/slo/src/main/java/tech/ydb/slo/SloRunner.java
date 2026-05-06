package tech.ydb.slo;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tech.ydb.core.Status;
import tech.ydb.jdbc.exception.YdbStatusable;
import tech.ydb.retry.YdbRetryProperties;

import java.time.Instant;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SloRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SloRunner.class);
    private static final String OPERATIONS_METRIC_NAME = "slo.operations";
    private static final String DURATION_METRIC_NAME = "slo.operation.duration.seconds";
    private static final String DURATION_METRIC_UNIT = "s";
    private static final List<Double> DURATION_BUCKETS = List.of(
            0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5,
            1.0, 2.5, 5.0, 10.0, 30.0
    );

    private static final String TABLE_NAME = "slo_test_table";
    private static final String READ_OPERATION = "read";
    private static final String WRITE_OPERATION = "write";
    private static final String SUCCESS_STATUS = "success";
    private static final String FAILURE_STATUS = "failure";
    private static final String NO_ERROR_TYPE = "none";

    private final JdbcTemplate jdbcTemplate;
    private final SloService sloService;
    private final SloConfig config;
    private final YdbRetryProperties retryProperties;
    private final SloResultWriter resultWriter;
    private final LongCounter operationsCounter;
    private final DoubleHistogram durationHistogram;
    private final SloStats sloStats = new SloStats();

    private final AtomicInteger nextId = new AtomicInteger(0);
    private final List<Integer> readableIds = Collections.synchronizedList(new ArrayList<>());

    private static final AttributeKey<String> REF_KEY = AttributeKey.stringKey("ref");
    private static final AttributeKey<String> OP_TYPE_KEY = AttributeKey.stringKey("operation_type");
    private static final AttributeKey<String> STATUS_KEY = AttributeKey.stringKey("status");
    private static final AttributeKey<String> ERROR_TYPE_KEY = AttributeKey.stringKey("error_type");

    public SloRunner(JdbcTemplate jdbcTemplate, SloService sloService, SloConfig config,
                     YdbRetryProperties retryProperties, SloResultWriter resultWriter,
                     OpenTelemetry openTelemetry) {
        this.jdbcTemplate = jdbcTemplate;
        this.sloService = sloService;
        this.config = config;
        this.retryProperties = retryProperties;
        this.resultWriter = resultWriter;

        Meter meter = openTelemetry.getMeter("slo");
        this.operationsCounter = meter.counterBuilder(OPERATIONS_METRIC_NAME)
                .setDescription("Total number of SLO operations")
                .build();
        this.durationHistogram = meter.histogramBuilder(DURATION_METRIC_NAME)
                .setDescription("SLO operation latency")
                .setUnit(DURATION_METRIC_UNIT)
                .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS)
                .build();
    }

    @Override
    public void run(String... args) {
        Instant startedAt = Instant.now();
        String runId = resultWriter.resolveRunId(config, startedAt);
        createTable();
        seedData();
        runWorkload(runId);
        Instant finishedAt = Instant.now();
        writeRunSummaryFile(runId, startedAt, finishedAt);
        waitForPrometheusScrapes(runId);
        log.info("SLO workload completed and final metrics were exposed for scraping: runId={}", runId);
    }

    private void createTable() {
        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                jdbcTemplate.execute(
                        "CREATE TABLE " + TABLE_NAME + " (" +
                                "guid Text, " +
                                "id Int32, " +
                                "payload_str Text, " +
                                "payload_double Double, " +
                                "payload_timestamp Timestamp, " +
                                "PRIMARY KEY (guid, id)" +
                                ")"
                );
                log.info("Created table {}", TABLE_NAME);
                return;
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg != null && (msg.contains("already exists") || msg.contains("ALREADY_EXISTS") || msg.contains("path exist"))) {
                    log.info("Table slo_test_table already exists");
                    return;
                }
                log.warn("Failed to create table (attempt {}/{}): {}", attempt + 1, 10, msg);
                if (attempt == 9) {
                    log.warn("Max attempts reached, proceeding anyway");
                    return;
                }
                try {
                    Thread.sleep((attempt + 1) * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
    }

    private void seedData() {
        log.info("Seeding {} initial rows...", config.getInitialDataCount());
        int success = 0;
        for (int i = 1; i <= config.getInitialDataCount(); i++) {
            try {
                String guid = guidFromInt(i);
                String payload = randomString();
                sloService.upsert(guid, i, payload, Math.random(), LocalDateTime.now());
                registerReadableId(i);
                success++;
            } catch (Exception e) {
                log.warn("Failed to seed row {}: {}", i, e.getMessage());
            }
        }
        nextId.set(config.getInitialDataCount());
        log.info("Seeded {}/{} rows", success, config.getInitialDataCount());
    }

    private void runWorkload(String runId) {
        String ref = config.getRef();
        log.info("Starting workload: runId={}, ref={}, readRps={}, writeRps={}, time={}s",
                runId, ref, config.getReadRps(), config.getWriteRps(), config.getRunTimeSeconds());

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        ExecutorService workers = Executors.newFixedThreadPool(20);

        int intervalMs = 100;
        int readsPerInterval = Math.max(1, config.getReadRps() / 10);
        int writesPerInterval = Math.max(1, config.getWriteRps() / 10);

        ScheduledFuture<?> readFuture = scheduler.scheduleAtFixedRate(() -> {
            for (int i = 0; i < readsPerInterval; i++) {
                workers.submit(() -> doRead(ref));
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);

        ScheduledFuture<?> writeFuture = scheduler.scheduleAtFixedRate(() -> {
            for (int i = 0; i < writesPerInterval; i++) {
                workers.submit(() -> doWrite(ref));
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);

        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(config.getRunTimeSeconds()));
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("SLO workload interrupted", interruptedException);
        }

        readFuture.cancel(false);
        writeFuture.cancel(false);
        scheduler.shutdown();
        workers.shutdown();
        awaitTermination("scheduler", scheduler, 30L, TimeUnit.SECONDS);
        awaitTermination("workers", workers, 30L, TimeUnit.SECONDS);
        log.info("Workload finished: runId={}, ref={}", runId, ref);
    }

    private void doWrite(String ref) {
        int id = nextId.incrementAndGet();
        String guid = guidFromInt(id);
        String payload = randomString();
        double payloadDouble = Math.random();
        LocalDateTime ts = LocalDateTime.now();

        long start = System.nanoTime();
        try {
            sloService.upsert2(guid, id, payload, payloadDouble, ts);
            registerReadableId(id);
            long durationNanos = System.nanoTime() - start;
            sloStats.recordSuccess(WRITE_OPERATION, durationNanos);
            recordLatency(ref, WRITE_OPERATION, SUCCESS_STATUS, NO_ERROR_TYPE, durationNanos);
            incrementCounter(ref, WRITE_OPERATION, SUCCESS_STATUS, NO_ERROR_TYPE);
        } catch (Exception e) {
            String errorType = extractErrorType(e);
            long durationNanos = System.nanoTime() - start;
            sloStats.recordFailure(WRITE_OPERATION, errorType, durationNanos);
            recordLatency(ref, WRITE_OPERATION, FAILURE_STATUS, errorType, durationNanos);
            incrementCounter(ref, WRITE_OPERATION, FAILURE_STATUS, errorType);
            log.debug("Write failed: [{}] {}", errorType, e.getMessage());
        }
    }

    private void doRead(String ref) {
        Integer id = pickReadableId();
        if (id == null) {
            return;
        }
        String guid = guidFromInt(id);

        long start = System.nanoTime();
        try {
            sloService.select(guid, id);
            long durationNanos = System.nanoTime() - start;
            sloStats.recordSuccess(READ_OPERATION, durationNanos);
            recordLatency(ref, READ_OPERATION, SUCCESS_STATUS, NO_ERROR_TYPE, durationNanos);
            incrementCounter(ref, READ_OPERATION, SUCCESS_STATUS, NO_ERROR_TYPE);
        } catch (Exception e) {
            String errorType = extractErrorType(e);
            long durationNanos = System.nanoTime() - start;
            sloStats.recordFailure(READ_OPERATION, errorType, durationNanos);
            recordLatency(ref, READ_OPERATION, FAILURE_STATUS, errorType, durationNanos);
            incrementCounter(ref, READ_OPERATION, FAILURE_STATUS, errorType);
            log.debug("Read failed: [{}] {}", errorType, e.getMessage());
        }
    }

    private void registerReadableId(int id) {
        readableIds.add(id);
    }

    private Integer pickReadableId() {
        synchronized (readableIds) {
            if (readableIds.isEmpty()) {
                return null;
            }
            return readableIds.get(ThreadLocalRandom.current().nextInt(readableIds.size()));
        }
    }

    private void incrementCounter(String ref, String operationType, String status, String errorType) {
        Attributes attrs = Attributes.builder()
                .put(REF_KEY, ref)
                .put(OP_TYPE_KEY, operationType)
                .put(STATUS_KEY, status)
                .put(ERROR_TYPE_KEY, errorType)
                .build();
        operationsCounter.add(1, attrs);
    }

    private void recordLatency(String ref, String operationType, String status, String errorType,
                                long durationNanos) {
        Attributes attrs = Attributes.builder()
                .put(REF_KEY, ref)
                .put(OP_TYPE_KEY, operationType)
                .put(STATUS_KEY, status)
                .put(ERROR_TYPE_KEY, errorType)
                .build();
        durationHistogram.record(durationNanos / 1_000_000_000.0, attrs);
    }

    static String extractErrorType(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof YdbStatusable statusable) {
                Status status = statusable.getStatus();
                if (status != null && status.getCode() != null) {
                    return status.getCode().name();
                }
            }
            current = current.getCause();
        }
        return throwable.getClass().getSimpleName();
    }

    static String guidFromInt(int value) {
        try {
            byte[] intBytes = new byte[4];
            intBytes[0] = (byte) (value >> 24);
            intBytes[1] = (byte) (value >> 16);
            intBytes[2] = (byte) (value >> 8);
            intBytes[3] = (byte) value;
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(intBytes);
            StringBuilder sb = new StringBuilder(36);
            for (int i = 0; i < 16; i++) {
                sb.append(String.format("%02x", hash[i]));
                if (i == 3 || i == 5 || i == 7 || i == 9) {
                    sb.append('-');
                }
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String randomString() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int len = 20 + rng.nextInt(21);
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char) (32 + rng.nextInt(95)));
        }
        return sb.toString();
    }

    private void writeRunSummaryFile(String runId, Instant startedAt, Instant finishedAt) {
        resultWriter.writeSummary(
                config,
                retryProperties,
                sloStats.calculate(runId, startedAt, finishedAt, sloStats)
        );
    }

    private void waitForPrometheusScrapes(String runId) {
        log.info(
                "Waiting {}s before shutdown to allow final Prometheus scrapes: runId={}",
                10,
                runId
        );
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for final Prometheus scrapes", interruptedException);
        }
    }

    private static void awaitTermination(String name, ExecutorService executorService, long timeout, TimeUnit unit) {
        try {
            if (!executorService.awaitTermination(timeout, unit)) {
                throw new IllegalStateException(name + " did not terminate in time");
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(name + " termination interrupted", interruptedException);
        }
    }

}
