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
        String nestedPath = "a".repeat(127) + "/" + "b".repeat(128);

        assertThat(YdbTablePath.fromUserInput("a".repeat(255)).value()).hasSize(255);
        assertThat(YdbTablePath.fromUserInput(nestedPath).value()).isEqualTo(nestedPath);

        assertThatThrownBy(() -> YdbTablePath.fromUserInput("a".repeat(256)))
                .isInstanceOf(TrinoException.class)
                .extracting(exception -> ((TrinoException) exception).getErrorCode())
                .isEqualTo(INVALID_ARGUMENTS.toErrorCode());

        assertThatThrownBy(() -> YdbTablePath.fromUserInput("a".repeat(256)))
                .hasMessageContaining("too long");
    }

    @Test
    void testPathDepth()
    {
        String maximumDepthPath = "a/".repeat(31) + "a";
        String excessiveDepthPath = maximumDepthPath + "/a";

        assertThat(YdbTablePath.fromUserInput(maximumDepthPath).value()).isEqualTo(maximumDepthPath);
        assertThatThrownBy(() -> YdbTablePath.fromUserInput(excessiveDepthPath))
                .isInstanceOf(TrinoException.class)
                .hasMessageContaining("too deep")
                .extracting(exception -> ((TrinoException) exception).getErrorCode())
                .isEqualTo(INVALID_ARGUMENTS.toErrorCode());

        assertThatThrownBy(() -> YdbTablePath.fromRemoteMetadata(excessiveDepthPath))
                .isInstanceOf(TrinoException.class)
                .extracting(exception -> ((TrinoException) exception).getErrorCode())
                .isEqualTo(JDBC_ERROR.toErrorCode());
    }

    @Test
    void testDataSystemTableName()
    {
        assertThat(YdbTablePath.isDataSystemTableName("nation$data")).isTrue();
        assertThat(YdbTablePath.isDataSystemTableName("a".repeat(255) + "$data")).isTrue();
        assertThat(YdbTablePath.isDataSystemTableName(
                "a".repeat(127) + "/" + "b".repeat(128) + "$data")).isTrue();

        for (String path : new String[] {"$data", "a/$data", "a$bad$data", "a".repeat(256) + "$data"}) {
            assertThat(YdbTablePath.isDataSystemTableName(path)).isFalse();
            assertThatThrownBy(() -> YdbTablePath.fromUserInput(path))
                    .isInstanceOf(TrinoException.class)
                    .extracting(exception -> ((TrinoException) exception).getErrorCode())
                    .isEqualTo(INVALID_ARGUMENTS.toErrorCode());
        }
    }

    @Test
    void testRemoteMetadataPaths()
    {
        String nestedPath = "a".repeat(127) + "/" + "b".repeat(128);

        assertThat(YdbTablePath.fromRemoteMetadata(".sys/table")).isEmpty();
        assertThat(YdbTablePath.fromRemoteMetadata(".sys/" + "a".repeat(251))).isEmpty();
        assertThat(YdbTablePath.fromRemoteMetadata(nestedPath)).contains(new YdbTablePath(nestedPath));

        assertThatThrownBy(() -> YdbTablePath.fromRemoteMetadata("a/$/b"))
                .isInstanceOf(TrinoException.class)
                .extracting(exception -> ((TrinoException) exception).getErrorCode())
                .isEqualTo(JDBC_ERROR.toErrorCode());

        assertThatThrownBy(() -> YdbTablePath.fromRemoteMetadata("a/" + "b".repeat(256)))
                .isInstanceOf(TrinoException.class)
                .extracting(exception -> ((TrinoException) exception).getErrorCode())
                .isEqualTo(JDBC_ERROR.toErrorCode());
    }
}
