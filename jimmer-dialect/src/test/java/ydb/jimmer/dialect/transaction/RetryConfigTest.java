package ydb.jimmer.dialect.transaction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RetryConfigTest {
    @Test
    public void ceilingCalculationTest() {
        RetryConfig config = RetryConfig.builder()
                .backoffBaseMs(1)
                .capBackoffMs(65)
                .backoffMultiplier(2)
                .build();

        Assertions.assertTrue(config.fastBackoffBaseMs() * Math.pow(2, config.fastCeiling())
                >= config.fastCapBackoffMs());
    }
}
