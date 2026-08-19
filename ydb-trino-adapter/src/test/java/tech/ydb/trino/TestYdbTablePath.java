package tech.ydb.trino;

import io.trino.spi.TrinoException;
import org.junit.jupiter.api.Test;

import static io.trino.plugin.jdbc.JdbcErrorCode.JDBC_ERROR;
import static io.trino.spi.StandardErrorCode.INVALID_ARGUMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestYdbTablePath
{
    @Test
    void testValidUserPaths()
    {
        assertThat(YdbTablePath.fromUserInput("orders").value()).isEqualTo("orders");
        assertThat(YdbTablePath.fromUserInput("sales/eu/orders").value()).isEqualTo("sales/eu/orders");
        assertThat(YdbTablePath.fromUserInput("sales.eu/orders-v2").comparisonKey())
                .isEqualTo("sales.eu/orders-v2");
    }

    @Test
    void testInvalidUserPaths()
    {
        for (String path : new String[] {"", "/orders", "orders/", "a//b", ".", "..", "a/../b", ".sys/table", "a/$/b"}) {
            assertThatThrownBy(() -> YdbTablePath.fromUserInput(path))
                    .isInstanceOf(TrinoException.class)
                    .extracting(exception -> ((TrinoException) exception).getErrorCode())
                    .isEqualTo(INVALID_ARGUMENTS.toErrorCode());
        }
    }

    @Test
    void testComponentLength()
    {
        assertThat(YdbTablePath.fromUserInput("a".repeat(255)).value()).hasSize(255);

        assertThatThrownBy(() -> YdbTablePath.fromUserInput("a".repeat(256)))
                .isInstanceOf(TrinoException.class)
                .extracting(exception -> ((TrinoException) exception).getErrorCode())
                .isEqualTo(INVALID_ARGUMENTS.toErrorCode());
    }

    @Test
    void testRemoteMetadataPaths()
    {
        assertThat(YdbTablePath.fromRemoteMetadata(".sys/table")).isEmpty();

        assertThatThrownBy(() -> YdbTablePath.fromRemoteMetadata("a/$/b"))
                .isInstanceOf(TrinoException.class)
                .extracting(exception -> ((TrinoException) exception).getErrorCode())
                .isEqualTo(JDBC_ERROR.toErrorCode());
    }
}
