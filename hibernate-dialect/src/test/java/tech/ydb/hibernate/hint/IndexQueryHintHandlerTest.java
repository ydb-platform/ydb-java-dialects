package tech.ydb.hibernate.hint;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.internal.QueryOptionsImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tech.ydb.hibernate.dialect.YdbDialect;

public class IndexQueryHintHandlerTest {

    @Nested
    class ShortForm {

        @Test
        void rewritesFromTable() {
            String query = "select g1_0.GroupId from Groups g1_0 where g1_0.GroupName='M3439'";
            String result = applyDatabaseHints(query, "use_index:group_name_index");

            assertEquals("select g1_0.GroupId from Groups view group_name_index g1_0 where g1_0.GroupName='M3439'", result);
        }

        @Test
        void multipleShortHintsShouldUseFirstIndex() {
            String query = "select g1_0.GroupId from Groups g1_0 where g1_0.GroupName='M3439'";
            String result = applyDatabaseHints(query, "use_index:idx_one", "use_index:idx_two");

            assertEquals("select g1_0.GroupId from Groups view idx_one g1_0 where g1_0.GroupName='M3439'", result);
        }

        @Test
        void noWhereClauseStillAppliesShortHint() {
            String query = "select g1_0.GroupId from Groups g1_0";
            String result = applyDatabaseHints(query, "use_index:group_name_index");

            assertEquals("select g1_0.GroupId from Groups view group_name_index g1_0", result);
        }

        @Test
        void shortHintAppliesWithoutAlias() {
            String query = "select GroupId from Groups where GroupName='M3439'";
            String result = applyDatabaseHints(query, "use_index:group_name_index");

            assertEquals("select GroupId from Groups view group_name_index where GroupName='M3439'", result);
        }

        @Test
        void hintsAreIgnoredWhenPrefixDoesNotMatch() {
            String query = "select g1_0.GroupId from Groups g1_0 where g1_0.GroupName='M3439'";
            String result = applyDatabaseHints(query, "use_scan", "add_pragma:Foo");

            assertEquals(
                    "PRAGMA Foo;\n" +
                            "scan select g1_0.GroupId from Groups g1_0 where g1_0.GroupName='M3439'",
                    result
            );
        }
    }

    @Nested
    class TypedFormSingleColumn {

        @Test
        void rewritesSingleJoin() {
            String query = "select * from orders o1_0 " +
                    "left join customers c1_0 on c1_0.code=o1_0.acc_dt_code " +
                    "where o1_0.id='X'";
            String result = applyDatabaseHints(query, "use_index:customers_code_idx:customers(code)");

            assertEquals(
                    "select * from orders o1_0 " +
                            "left join customers view customers_code_idx c1_0 on c1_0.code=o1_0.acc_dt_code " +
                            "where o1_0.id='X'",
                    result
            );
        }

        @Test
        void rewritesTwoJoinsOfSameTableOnDifferentColumns() {
            String query = "select * from orders o1_0 " +
                    "left join customers c1_0 on c1_0.code=source.code " +
                    "left join customers c3_0 on c3_0.parent=source.parent " +
                    "where o1_0.id='X'";
            String result = applyDatabaseHints(query, "use_index:customers_code_idx:customers(code)", "use_index:customers_parent_idx:customers(parent)");

            assertEquals(
                    "select * from orders o1_0 " +
                            "left join customers view customers_code_idx c1_0 on c1_0.code=source.code " +
                            "left join customers view customers_parent_idx c3_0 on c3_0.parent=source.parent " +
                            "where o1_0.id='X'",
                    result
            );
        }

        @Test
        void skipsJoinsWhereColumnIsAbsent() {
            String query = "select * from orders o1_0 " +
                    "left join customers c1_0 on c1_0.id=o1_0.acc_dt_id " +
                    "where o1_0.id='X'";
            String result = applyDatabaseHints(query, "use_index:customers_code_idx:customers(code)");

            assertEquals(query, result);
        }

        @Test
        void leavesUnrelatedTablesUntouched() {
            String query = "select * from orders o1_0 " +
                    "left join customers c1_0 on c1_0.code=source.code " +
                    "left join regions r1_0 on r1_0.code=source.code " +
                    "where o1_0.id='X'";
            String result = applyDatabaseHints(query, "use_index:customers_code_idx:customers(code)");

            assertEquals(
                    "select * from orders o1_0 " +
                            "left join customers view customers_code_idx c1_0 on c1_0.code=source.code " +
                            "left join regions r1_0 on r1_0.code=source.code " +
                            "where o1_0.id='X'",
                    result
            );
        }

        @Test
        void typedFromHintDoesNotRewriteFromTable() {
            String query = "select g1_0.GroupId from Groups g1_0 where g1_0.GroupName='M3439'";
            String result = applyDatabaseHints(query, "use_index:group_name_index:Groups(GroupName)");

            assertEquals(query, result);
        }
    }

    @Nested
    class TypedFormCompositeColumns {


        @Test
        void fullColumnMatch_appliesIndex() {
            String query = "select * from orders o1_0 " +
                    "left join customers c1_0 on c1_0.code=source.code and c1_0.parent=source.parent " +
                    "where o1_0.id='X'";
            String result = applyDatabaseHints(query, "use_index:customers_combo_idx:customers(code,parent)");

            assertEquals(
                    "select * from orders o1_0 " +
                            "left join customers view customers_combo_idx c1_0 on c1_0.code=source.code and c1_0.parent=source.parent " +
                            "where o1_0.id='X'",
                    result
            );
        }


        @Test
        void partialColumnMatch_doesNotApply() {
            String query = "select * from orders o1_0 " +
                    "left join customers c1_0 on c1_0.code=source.code " +
                    "left join customers c3_0 on c3_0.parent=source.parent " +
                    "where o1_0.id='X'";
            String result = applyDatabaseHints(query, "use_index:customers_combo_idx:customers(code,parent)");

            assertEquals(query, result);
        }


        @Test
        void bestMatchWins_mostSpecificIndexPicked() {
            String query = "select * from orders o1_0 " +
                    "left join customers c1_0 on c1_0.code=source.code and c1_0.parent=source.parent " +
                    "where o1_0.id='X'";
            String result = applyDatabaseHints(query, "use_index:customers_code_idx:customers(code)", "use_index:customers_combo_idx:customers(code,parent)");

            assertEquals(
                    "select * from orders o1_0 " +
                            "left join customers view customers_combo_idx c1_0 on c1_0.code=source.code and c1_0.parent=source.parent " +
                            "where o1_0.id='X'",
                    result
            );
        }


        @Test
        void partialCompositeFallsBackToFullSingleColumn() {
            String query = "select * from orders o1_0 " +
                    "left join customers c1_0 on c1_0.code=source.code " +
                    "where o1_0.id='X'";
            String result = applyDatabaseHints(query, "use_index:customers_combo_idx:customers(code,parent)", "use_index:customers_code_idx:customers(code)");

            assertEquals(
                    "select * from orders o1_0 " +
                            "left join customers view customers_code_idx c1_0 on c1_0.code=source.code " +
                            "where o1_0.id='X'",
                    result
            );
        }
    }

    @Nested
    class IdentifierBoundaries {


        @Test
        void columnNameIsNotASubstringMatch() {
            String query = "select * from orders o1_0 " +
                    "left join customers c1_0 on c1_0.acc_dt_code=source.x " +
                    "left join customers c2_0 on c2_0.code1=source.y " +
                    "where o1_0.id='X'";
            String result = applyDatabaseHints(query, "use_index:customers_code_idx:customers(code)");

            assertEquals(query, result);
        }


        @Test
        void aliasIsNotASubstringMatch() {
            String query = "select * from orders o1_0 " +
                    "left join customers c1_0 on xo1_0.code=source.code " +
                    "where o1_0.id='X'";
            String result = applyDatabaseHints(query, "use_index:customers_code_idx:customers(code)");

            assertEquals(query, result);
        }
    }

    @Nested
    class Interactions {

        @Test
        void typedFromHintDoesNotBlockShortForm() {
            String query = "select g1_0.GroupId from Groups g1_0 where g1_0.GroupName='M3439'";
            String result = applyDatabaseHints(query, "use_index:group_name_index:Groups(GroupName)", "use_index:other_index");

            assertEquals(
                    "select g1_0.GroupId from Groups view other_index g1_0 where g1_0.GroupName='M3439'",
                    result
            );
        }

        @Test
        void largeRealisticQueryWithMultipleAliasesOfSameTable() {
            String query = "select o1_0.id,c1_0.id,c3_0.id,r1_0.id " +
                    "from orders o1_0 " +
                    "left join customers c1_0 on c1_0.id=o1_0.acc_dt_id " +
                    "left join customers c3_0 on c3_0.id=o1_0.acc_kt_id " +
                    "left join regions r1_0 on r1_0.id=o1_0.branch_id " +
                    "where o1_0.id='abc'";
            String result = applyDatabaseHints(query, "use_index:customers_id_idx:customers(id)", "use_index:regions_id_idx:regions(id)");

            assertEquals(
                    "select o1_0.id,c1_0.id,c3_0.id,r1_0.id " +
                            "from orders o1_0 " +
                            "left join customers view customers_id_idx c1_0 on c1_0.id=o1_0.acc_dt_id " +
                            "left join customers view customers_id_idx c3_0 on c3_0.id=o1_0.acc_kt_id " +
                            "left join regions view regions_id_idx r1_0 on r1_0.id=o1_0.branch_id " +
                            "where o1_0.id='abc'",
                    result
            );
        }

        @Test
        void malformedTypedHintIsIgnored() {
            String query = "select g1_0.GroupId from Groups g1_0 where g1_0.GroupName='M3439'";
            String result = applyDatabaseHints(query, "use_index:group_name_index:Groups(GroupName");

            assertTrue(result.contains("from Groups"));
        }
    }


    @Nested
    class TopLevelClauseBoundaries {

        @Test
        void joinKeywordInsideStringLiteral_doesNotCreateSpuriousJoin() {
            String query = "select * from orders o1_0 " +
                    "left join customers c1_0 on c1_0.code=o1_0.x " +
                    "where o1_0.note='text with left join customers fake on fake.code=x'";
            String result = applyDatabaseHints(query, "use_index:customers_code_idx:customers(code)");

            assertEquals(
                    "select * from orders o1_0 " +
                            "left join customers view customers_code_idx c1_0 on c1_0.code=o1_0.x " +
                            "where o1_0.note='text with left join customers fake on fake.code=x'",
                    result
            );
        }

        @Test
        void fromInsideSubquery_isNotRewritten() {
            String query = "select * from orders o1_0 " +
                    "left join (select x.id from customers x where x.parent='p') c1_0 on c1_0.id=o1_0.id " +
                    "left join customers c2_0 on c2_0.code=o1_0.code " +
                    "where o1_0.id='X'";
            String result = applyDatabaseHints(query, "use_index:customers_code_idx:customers(code)", "use_index:customers_parent_idx:customers(parent)");

            assertTrue(result.contains("left join customers view customers_code_idx c2_0 on c2_0.code"));
            assertFalse(result.contains("customers view customers_code_idx c1_0"));
            assertFalse(result.contains("customers view customers_parent_idx"));
            assertFalse(result.contains("from customers view"));
        }

        @Test
        void joinInsideBlockComment_doesNotSplitSegments() {
            String query = "select * from orders o1_0 /* join decoy */ " +
                    "left join customers c1_0 on c1_0.code=o1_0.x where o1_0.id='X'";
            String result = applyDatabaseHints(query, "use_index:customers_code_idx:customers(code)");

            assertEquals(
                    "select * from orders o1_0 /* join decoy */ " +
                            "left join customers view customers_code_idx c1_0 on c1_0.code=o1_0.x where o1_0.id='X'",
                    result
            );
        }

        @Test
        void viewIsInsertedBeforeAsAlias() {
            String query = "select a1.b as a1_b, a2.b as a2_b " +
                    "from a as a1 join a as a2 on a1.d=a2.c where a1.d=1";
            String result = applyDatabaseHints(query,
                    "use_index:Index1",
                    "use_index:Index2:a(c)");

            assertEquals(
                    "select a1.b as a1_b, a2.b as a2_b " +
                            "from a view Index1 as a1 join a view Index2 as a2 on a1.d=a2.c where a1.d=1",
                    result
            );
        }
    }

    private static String applyDatabaseHints(String query, String... hints) {
        QueryOptionsImpl options = new QueryOptionsImpl();
        for (String hint : hints) {
            options.addDatabaseHint(hint);
        }

        return DIALECT.addSqlHintOrComment(query, options, false);
    }

    private static final YdbDialect DIALECT = new YdbDialect(new DialectResolutionInfo() {
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
    });
}
