package tech.ydb.slo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.ydb.retry.YdbRetryProperties;

@Component
public class SloResultWriter {

    private static final Logger log = LoggerFactory.getLogger(SloResultWriter.class);
    private static final String CURRENT_RUN_ID_FILE = ".current-run-id";
    private static final String RUN_ID_PREFIX = "run-";
    private static final String RUN_ID_TIMESTAMP_PATTERN = "yyyyMMdd-HHmmss";
    private static final int RUN_ID_RANDOM_SUFFIX_LENGTH = 8;
    private static final String WITH_RETRY_REF = "with-retry";
    private static final String NO_RETRY_REF = "no-retry";
    private static final String RETRY_RESULT_FILE_NAME = "retry";
    private static final String NO_RETRY_RESULT_FILE_NAME = "no-retry";
    private static final String FILE_NAME_SANITIZE_REGEX = "[^a-zA-Z0-9._-]";
    private static final String FILE_NAME_SANITIZE_REPLACEMENT = "_";

    public String resolveRunId(SloConfig config, Instant startedAt) {
        if (config.getRunId() != null && !config.getRunId().isBlank()) {
            return config.getRunId();
        }

        Path resultsRoot = resultsRoot(config);
        try {
            Files.createDirectories(resultsRoot);
            Path currentRunIdFile = resultsRoot.resolve(CURRENT_RUN_ID_FILE);
            try (FileChannel channel =
                         FileChannel.open(
                                 currentRunIdFile,
                                 StandardOpenOption.CREATE,
                                 StandardOpenOption.READ,
                                 StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                String existingRunId = readCurrentRunId(channel);
                if (!existingRunId.isBlank() && isReusableRun(resultsRoot.resolve(existingRunId))) {
                    return existingRunId;
                }

                String generatedRunId = generateRunId(startedAt);
                writeCurrentRunId(channel, generatedRunId);
                return generatedRunId;
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to resolve shared SLO runId", exception);
        }
    }

    public void writeSummary(
            SloConfig config, YdbRetryProperties retryProperties, RunSummary summary) {
        Path runDirectory = resultsRoot(config).resolve(summary.runId());
        Path resultFile = runDirectory.resolve(resultFileName(config.getRef()));

        try {
            Files.createDirectories(runDirectory);
            Files.writeString(
                    resultFile,
                    buildRunSummaryText(config, retryProperties, summary),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            log.info(
                    "SLO run result file written: runId={}, ref={}, path={}",
                    summary.runId(),
                    config.getRef(),
                    resultFile.toAbsolutePath());
        } catch (IOException exception) {
            throw new RuntimeException("Failed to write SLO run summary file", exception);
        }
    }

    public Path resultsRoot(SloConfig config) {
        return Path.of(config.getResultsDir());
    }

    private String generateRunId(Instant startedAt) {
        String timestamp =
                DateTimeFormatter.ofPattern(RUN_ID_TIMESTAMP_PATTERN)
                        .withZone(ZoneOffset.UTC)
                        .format(startedAt);
        return RUN_ID_PREFIX
                + timestamp
                + "-"
                + UUID.randomUUID().toString().substring(0, RUN_ID_RANDOM_SUFFIX_LENGTH);
    }

    private String buildRunSummaryText(
            SloConfig config, YdbRetryProperties retryProperties, RunSummary summary) {
        StringBuilder builder = new StringBuilder();
        builder.append("runId: ").append(summary.runId()).append('\n');
        builder.append("ref: ").append(config.getRef()).append('\n');
        builder.append("startedAt: ").append(summary.startedAt()).append('\n');
        builder.append("finishedAt: ").append(summary.finishedAt()).append('\n');
        builder.append("durationMs: ")
                .append(Duration.between(summary.startedAt(), summary.finishedAt()).toMillis())
                .append('\n');
        builder.append("resultsDir: ").append(resultsRoot(config).toAbsolutePath()).append('\n');
        builder.append('\n');
        builder.append("readRps: ").append(config.getReadRps()).append('\n');
        builder.append("writeRps: ").append(config.getWriteRps()).append('\n');
        builder.append("initialDataCount: ").append(config.getInitialDataCount()).append('\n');
        builder.append("runTimeSeconds: ").append(config.getRunTimeSeconds()).append('\n');
        builder.append('\n');
        builder.append("retryEnabled: ").append(retryProperties.isEnabled()).append('\n');
        builder.append("retryMaxRetries: ").append(retryProperties.getMaxRetries()).append('\n');
        builder.append("retrySlowBackoffBaseMs: ")
                .append(retryProperties.getSlowBackoffBaseMs())
                .append('\n');
        builder.append("retryFastBackoffBaseMs: ")
                .append(retryProperties.getFastBackoffBaseMs())
                .append('\n');
        builder.append("retrySlowCapBackoffMs: ")
                .append(retryProperties.getSlowCapBackoffMs())
                .append('\n');
        builder.append("retryFastCapBackoffMs: ")
                .append(retryProperties.getFastCapBackoffMs())
                .append('\n');
        builder.append('\n');
        builder.append("totalOperations: ").append(summary.totalOperations()).append('\n');
        builder.append("totalSuccess: ").append(summary.totalSuccess()).append('\n');
        builder.append("totalFailure: ").append(summary.totalFailure()).append('\n');
        builder.append("failureRatePercent: ").append(summary.failureRatePercent()).append('\n');
        builder.append("readSuccess: ").append(summary.readSuccess()).append('\n');
        builder.append("readFailure: ").append(summary.readFailure()).append('\n');
        builder.append("writeSuccess: ").append(summary.writeSuccess()).append('\n');
        builder.append("writeFailure: ").append(summary.writeFailure()).append('\n');
        builder.append('\n');
        builder.append("overallP50Ms: ").append(summary.overallP50()).append('\n');
        builder.append("overallP95Ms: ").append(summary.overallP95()).append('\n');
        builder.append("overallP99Ms: ").append(summary.overallP99()).append('\n');
        builder.append("readP50Ms: ").append(summary.readP50()).append('\n');
        builder.append("readP95Ms: ").append(summary.readP95()).append('\n');
        builder.append("readP99Ms: ").append(summary.readP99()).append('\n');
        builder.append("writeP50Ms: ").append(summary.writeP50()).append('\n');
        builder.append("writeP95Ms: ").append(summary.writeP95()).append('\n');
        builder.append("writeP99Ms: ").append(summary.writeP99()).append('\n');
        builder.append('\n');
        builder.append("errorTypes:").append('\n');
        if (summary.errorCounts().isEmpty()) {
            builder.append("  none").append('\n');
        } else {
            summary.errorCounts()
                    .forEach(
                            (errorType, count) ->
                                    builder.append("  ").append(errorType).append(": ").append(count).append('\n'));
        }
        return builder.toString();
    }

    private static String resultFileName(String ref) {
        if (WITH_RETRY_REF.equals(ref)) {
            return RETRY_RESULT_FILE_NAME;
        }
        if (NO_RETRY_REF.equals(ref)) {
            return NO_RETRY_RESULT_FILE_NAME;
        }
        return ref.replaceAll(FILE_NAME_SANITIZE_REGEX, FILE_NAME_SANITIZE_REPLACEMENT);
    }

    private static boolean isReusableRun(Path runDirectory) {
        return !Files.exists(runDirectory.resolve(RETRY_RESULT_FILE_NAME))
                && !Files.exists(runDirectory.resolve(NO_RETRY_RESULT_FILE_NAME));
    }

    private static String readCurrentRunId(FileChannel channel) throws IOException {
        channel.position(0);
        ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
        channel.read(buffer);
        buffer.flip();
        return StandardCharsets.UTF_8.decode(buffer).toString().trim();
    }

    private static void writeCurrentRunId(FileChannel channel, String runId) throws IOException {
        channel.truncate(0);
        channel.position(0);
        channel.write(StandardCharsets.UTF_8.encode(runId));
        channel.force(true);
    }

    public record RunSummary(
            String runId,
            Instant startedAt,
            Instant finishedAt,
            long totalOperations,
            long totalSuccess,
            long totalFailure,
            String failureRatePercent,
            long readSuccess,
            long readFailure,
            long writeSuccess,
            long writeFailure,
            String overallP50,
            String overallP95,
            String overallP99,
            String readP50,
            String readP95,
            String readP99,
            String writeP50,
            String writeP95,
            String writeP99,
            Map<String, Long> errorCounts) {
    }
}
