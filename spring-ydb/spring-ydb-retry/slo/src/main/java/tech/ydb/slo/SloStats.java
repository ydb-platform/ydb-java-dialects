package tech.ydb.slo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class SloStats {

    private static final String READ_OPERATION = "read";
    private static final String EMPTY_PERCENTILE = "n/a";
    private static final String FAILURE_RATE_FORMAT = "%.4f";
    private static final String LATENCY_FORMAT = "%.3f";
    private static final double PERCENTILE_50 = 0.50;
    private static final double PERCENTILE_95 = 0.95;
    private static final double PERCENTILE_99 = 0.99;
    private static final double NANOS_IN_MILLISECOND = 1_000_000.0;
    private static final double PERCENT_FACTOR = 100.0;

    private final AtomicLong readSuccess = new AtomicLong();
    private final AtomicLong readFailure = new AtomicLong();
    private final AtomicLong writeSuccess = new AtomicLong();
    private final AtomicLong writeFailure = new AtomicLong();
    private final List<Long> overallLatenciesNanos = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> readLatenciesNanos = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> writeLatenciesNanos = Collections.synchronizedList(new ArrayList<>());
    private final ConcurrentHashMap<String, LongAdder> errorCounts = new ConcurrentHashMap<>();

    public void recordSuccess(String operationType, long durationNanos) {
        recordLatency(operationType, durationNanos);
        if (READ_OPERATION.equals(operationType)) {
            readSuccess.incrementAndGet();
            return;
        }
        writeSuccess.incrementAndGet();
    }

    public void recordFailure(String operationType, String errorType, long durationNanos) {
        recordLatency(operationType, durationNanos);
        errorCounts.computeIfAbsent(errorType, ignored -> new LongAdder()).increment();
        if (READ_OPERATION.equals(operationType)) {
            readFailure.incrementAndGet();
            return;
        }
        writeFailure.incrementAndGet();
    }

    public long getReadSuccess() {
        return readSuccess.get();
    }

    public long getReadFailure() {
        return readFailure.get();
    }

    public long getWriteSuccess() {
        return writeSuccess.get();
    }

    public long getWriteFailure() {
        return writeFailure.get();
    }

    public List<Long> overallLatenciesSnapshot() {
        return snapshotLatencies(overallLatenciesNanos);
    }

    public List<Long> readLatenciesSnapshot() {
        return snapshotLatencies(readLatenciesNanos);
    }

    public List<Long> writeLatenciesSnapshot() {
        return snapshotLatencies(writeLatenciesNanos);
    }

    public Map<String, Long> errorCountsSnapshot() {
        return errorCounts.entrySet().stream()
                .sorted(Map.Entry.<String, LongAdder>comparingByValue(
                        Comparator.comparingLong(LongAdder::sum)
                ).reversed())
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue().sum()),
                        LinkedHashMap::putAll);
    }

    private void recordLatency(String operationType, long durationNanos) {
        overallLatenciesNanos.add(durationNanos);
        if (READ_OPERATION.equals(operationType)) {
            readLatenciesNanos.add(durationNanos);
            return;
        }
        writeLatenciesNanos.add(durationNanos);
    }

    private static List<Long> snapshotLatencies(List<Long> latenciesNanos) {
        synchronized (latenciesNanos) {
            return new ArrayList<>(latenciesNanos);
        }
    }

    public SloResultWriter.RunSummary calculate(String runId,
                                                Instant startedAt,
                                                Instant finishedAt,
                                                SloStats runStats) {
        long readSuccess = runStats.getReadSuccess();
        long readFailure = runStats.getReadFailure();
        long writeSuccess = runStats.getWriteSuccess();
        long writeFailure = runStats.getWriteFailure();
        long totalSuccess = readSuccess + writeSuccess;
        long totalFailure = readFailure + writeFailure;
        long totalOperations = totalSuccess + totalFailure;
        double failureRatePercent = totalOperations == 0 ? 0.0 : (double) totalFailure * PERCENT_FACTOR / totalOperations;

        return new SloResultWriter.RunSummary(
                runId,
                startedAt,
                finishedAt,
                totalOperations,
                totalSuccess,
                totalFailure,
                String.format(Locale.ROOT, FAILURE_RATE_FORMAT, failureRatePercent),
                readSuccess,
                readFailure,
                writeSuccess,
                writeFailure,
                formatPercentileMillis(runStats.overallLatenciesSnapshot(), PERCENTILE_50),
                formatPercentileMillis(runStats.overallLatenciesSnapshot(), PERCENTILE_95),
                formatPercentileMillis(runStats.overallLatenciesSnapshot(), PERCENTILE_99),
                formatPercentileMillis(runStats.readLatenciesSnapshot(), PERCENTILE_50),
                formatPercentileMillis(runStats.readLatenciesSnapshot(), PERCENTILE_95),
                formatPercentileMillis(runStats.readLatenciesSnapshot(), PERCENTILE_99),
                formatPercentileMillis(runStats.writeLatenciesSnapshot(), PERCENTILE_50),
                formatPercentileMillis(runStats.writeLatenciesSnapshot(), PERCENTILE_95),
                formatPercentileMillis(runStats.writeLatenciesSnapshot(), PERCENTILE_99),
                runStats.errorCountsSnapshot()
        );
    }

    private static String formatPercentileMillis(List<Long> latenciesNanos, double percentile) {
        if (latenciesNanos.isEmpty()) {
            return EMPTY_PERCENTILE;
        }
        latenciesNanos.sort(Long::compareTo);
        int index = Math.min(latenciesNanos.size() - 1, (int) Math.ceil(percentile * latenciesNanos.size()) - 1);
        double millis = latenciesNanos.get(index) / NANOS_IN_MILLISECOND;
        return String.format(Locale.ROOT, LATENCY_FORMAT, millis);
    }
}
