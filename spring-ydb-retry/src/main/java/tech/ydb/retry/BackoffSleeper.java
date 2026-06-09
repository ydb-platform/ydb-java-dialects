package tech.ydb.retry;

@FunctionalInterface
public interface BackoffSleeper {
    void sleep(long delayMs) throws InterruptedException;
}
