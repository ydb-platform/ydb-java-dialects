package tech.ydb.retry.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

class ConcurrentRunner {

    private final int threadCount;
    private final ExecutorService executor;
    private final CyclicBarrier barrier;
    private final List<Future<?>> futures = new ArrayList<>();
    private final AtomicInteger successCount = new AtomicInteger();
    private final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

    private ConcurrentRunner(int threadCount) {
        this.threadCount = threadCount;
        this.executor = Executors.newFixedThreadPool(threadCount);
        this.barrier = new CyclicBarrier(threadCount);
    }

    static ConcurrentRunner with(int threadCount) {
        return new ConcurrentRunner(threadCount);
    }

    ConcurrentRunner execute(IntConsumer task) {
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    task.accept(idx);
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    errors.add(t);
                }
            }));
        }
        return this;
    }

    ConcurrentResult awaitCompletion(long timeout, TimeUnit unit) throws Exception {
        for (Future<?> f : futures) {
            f.get(timeout, unit);
        }
        executor.shutdown();
        return new ConcurrentResult(successCount.get(), errors);
    }

    record ConcurrentResult(int successCount, List<Throwable> errors) {
        void assertAllSucceeded() {
            if (!errors.isEmpty()) {
                RuntimeException ex = new RuntimeException("Concurrent test had " + errors.size() + " failures");
                errors.forEach(ex::addSuppressed);
                throw ex;
            }
            if (successCount == 0) {
                throw new RuntimeException("No threads succeeded");
            }
        }

        void assertSuccessCount(int expected) {
            if (successCount != expected) {
                throw new RuntimeException("Expected " + expected + " successes but got " + successCount);
            }
        }
    }
}
