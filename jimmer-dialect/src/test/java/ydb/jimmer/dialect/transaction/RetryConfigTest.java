package ydb.jimmer.dialect.transaction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class RetryConfigTest {
    @Test
    public void ceilingCalculationTest() {
        RetryConfig.Builder cb = RetryConfig.builder();

        int[] backoff = new int[] {1, 2, 3, 10};
        int[] cap = new int[] {63, 64, 65, 80, 81, 82, 100};
        int[] multiplier = new int[] {2, 3, 10};
        for (int j : backoff) {
            for (int k : cap) {
                for (int value : multiplier) {
                    checkCeilingValue(cb
                            .backoffBaseMs(j)
                            .capBackoffMs(k)
                            .backoffMultiplier(value)
                            .build());
                }
            }
        }
    }

    private static void checkCeilingValue(RetryConfig config) {
        Assertions.assertTrue(
                config.fastBackoffBaseMs() * Math.pow(config.backoffMultiplier(), config.fastCeiling())
                >= config.fastCapBackoffMs());
    }

    @Test
    public void illegalArgumentsCheck() {
        RetryConfig.Builder cb = RetryConfig.builder();

        int[] values = new int[] {0, -1, -10};
        for (int v : values) {
            checkIfThrows(() -> cb
                    .maxAttempts(v)
                    .build());
        }

        values = new int[] {-1, -2, -10};
        for (int v : values) {
            checkIfThrows(() -> cb
                    .backoffBaseMs(v)
                    .build());

            checkIfThrows(() -> cb
                    .capBackoffMs(v)
                    .build());
        }

        double[] dvalues = new double[] {1, 0.5, 0, -0.5, -1, -10};
        for (double v : dvalues) {
            checkIfThrows(() -> cb
                    .backoffMultiplier(v)
                    .build());
        }
    }

    private static void checkIfThrows(Executable block) {
        Assertions.assertThrows(IllegalArgumentException.class, block);
    }
}
