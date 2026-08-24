package tech.ydb.trino;

import org.junit.jupiter.api.Test;
import tech.ydb.jdbc.common.YdbTypes;
import tech.ydb.jdbc.query.QueryKey;
import tech.ydb.jdbc.query.YdbQuery;
import tech.ydb.jdbc.settings.YdbQueryProperties;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class TestYdbJdbcBatching
{
    @Test
    void testQueryCommentPreservesAutoBatchRecognition()
            throws Exception
    {
        YdbTypes types = new YdbTypes(false);
        YdbQueryProperties properties = new YdbQueryProperties(new Properties());

        for (String sql : List.of(
                "DELETE FROM `orders` WHERE `account` = ? AND `region` = ? /*catalog=ydb*/",
                "UPDATE `orders` SET `note` = ? WHERE `account` = ? AND `region` = ? /*catalog=ydb*/",
                "INSERT INTO `orders` (`account`, `region`, `note`) VALUES (?, ?, ?) /*catalog=ydb*/")) {
            YdbQuery query = YdbQuery.parseQuery(new QueryKey(sql), properties, types);

            assertThat(query.getYqlBatcher())
                    .as("auto-batch parser for %s", sql)
                    .isNotNull();
        }
    }
}
