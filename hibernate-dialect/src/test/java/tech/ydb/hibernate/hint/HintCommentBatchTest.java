package tech.ydb.hibernate.hint;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import tech.ydb.hibernate.dialect.YdbDialect;

public class HintCommentBatchTest {

    @Test
    void multipleShortUseIndexHintsInComment_areJoinedWithComma() {
        QueryOptionsImpl options = new QueryOptionsImpl();
        options.setComment("use_index:idx_one;use_index:idx_two");

        String sql = "select g1_0.GroupId from Groups g1_0 where g1_0.GroupName='M3439'";
        String result = new YdbDialect(resolutionInfo()).addSqlHintOrComment(sql, options, false);

        assertEquals(
                "select g1_0.GroupId from Groups view idx_one, idx_two g1_0 where g1_0.GroupName='M3439'",
                result
        );
        assertFalse(result.contains("view idx_one view"));
        assertFalse(result.contains("view idx_two view"));
    }

    private static DialectResolutionInfo resolutionInfo() {
        return new DialectResolutionInfo() {
            @Override
            public String getDatabaseName() {
                return "YDB";
            }

            @Override
            public String getDatabaseVersion() {
                return "1.0";
            }

            @Override
            public String getDriverName() {
                return "ydb";
            }

            @Override
            public int getDriverMajorVersion() {
                return 2;
            }

            @Override
            public int getDriverMinorVersion() {
                return 0;
            }

            @Override
            public String getSQLKeywords() {
                return "";
            }

            @Override
            public int getDatabaseMajorVersion() {
                return 1;
            }

            @Override
            public int getDatabaseMinorVersion() {
                return 0;
            }
        };
    }
}
