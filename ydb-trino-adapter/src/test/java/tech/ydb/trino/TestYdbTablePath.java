package tech.ydb.trino;

import io.trino.spi.TrinoException;
import org.junit.jupiter.api.Test;

import static io.trino.spi.StandardErrorCode.INVALID_ARGUMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestYdbTablePath
{
    @Test
    void testRelativePaths()
    {
        // YDB owns component naming and size limits; the connector only checks path structure.
        for (String path : new String[] {"orders", "sales/eu/orders", ".hidden", "a/$/b",
                "a".repeat(256), "a/".repeat(32) + "a"}) {
            assertThat(YdbTablePath.fromUserInput(path).value()).isEqualTo(path);
            assertThat(YdbTablePath.lookup(path)).contains(new YdbTablePath(path));
        }
        assertThat(YdbTablePath.fromUserInput("Sales/EU/Orders").comparisonKey()).isEqualTo("sales/eu/orders");
    }

    @Test
    void testInvalidPaths()
    {
        for (String path : new String[] {"", "/orders", "orders/", "a//b", ".", "..", "a/./b", "a/../b"}) {
            assertThat(YdbTablePath.lookup(path)).as(path).isEmpty();
            assertThatThrownBy(() -> YdbTablePath.fromUserInput(path))
                    .as(path)
                    .isInstanceOf(TrinoException.class)
                    .extracting(exception -> ((TrinoException) exception).getErrorCode())
                    .isEqualTo(INVALID_ARGUMENTS.toErrorCode());
        }
    }
}
