package tech.ydb.hibernate.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.query.Query;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import tech.ydb.hibernate.TestUtils;
import tech.ydb.test.junit5.YdbHelperExtension;

/**
 * @author Kirill Kurdyukov
 */
public class BankDocumentIndexHintTest {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    private static SessionFactory sessionFactory;

    @BeforeAll
    static void beforeAll() {
        Configuration configuration = TestUtils.basedConfiguration()
                .setProperty(AvailableSettings.URL, TestUtils.jdbcUrl(ydb))
                .setProperty(AvailableSettings.STATEMENT_INSPECTOR, CapturingStatementInspector.class.getName())
                .addAnnotatedClass(BankAccount.class)
                .addAnnotatedClass(BankDocument.class);

        sessionFactory = configuration.buildSessionFactory();

        sessionFactory.inTransaction(session -> {
            BankAccount a1 = newAccount(1, "ACC-CODE-1", "PARENT-1", "first");
            BankAccount a2 = newAccount(2, "ACC-CODE-2", "PARENT-2", "second");
            session.persist(a1);
            session.persist(a2);

            BankDocument document = new BankDocument();
            document.setId(1);
            document.setAccDt(a1);
            document.setAccKt(a2);
            document.setAccCombo(a1);
            session.persist(document);
        });
    }

    @BeforeEach
    void clearCapturedSql() {
        CapturingStatementInspector.clear();
    }

    
    @Test
    void sameTableJoinedTwice_byDifferentColumns_getsTwoDifferentIndexes() {
        String joins = "join fetch d.accDt join fetch d.accKt";
        String hintCode = "use_index:bank_account_code_idx:bank_account(code)";
        String hintParent = "use_index:bank_account_parent_idx:bank_account(parent)";

        runDocumentQuery(joins, query -> query.addQueryHint(hintCode).addQueryHint(hintParent));
        assertDocumentSql(sql -> {
            assertTrue(sql.contains("bank_account view bank_account_code_idx"),
                    () -> "expected code index on the code join, got:\n" + sql);
            assertTrue(sql.contains("bank_account view bank_account_parent_idx"),
                    () -> "expected parent index on the parent join, got:\n" + sql);
        });

        clearCapturedSql();
        runDocumentQuery(joins, query -> query.setHint(HibernateHints.HINT_COMMENT, hintCode + ";" + hintParent));
        assertDocumentSql(sql -> {
            assertTrue(sql.contains("bank_account view bank_account_code_idx"),
                    () -> "expected code index via HINT_COMMENT, got:\n" + sql);
            assertTrue(sql.contains("bank_account view bank_account_parent_idx"),
                    () -> "expected parent index via HINT_COMMENT, got:\n" + sql);
        });
    }

    
    @Test
    void compositeJoin_picksIndexWithMostColumnMatches() {
        String joins = "join fetch d.accCombo";
        String hintCode = "use_index:bank_account_code_idx:bank_account(code)";
        String hintCombo = "use_index:bank_account_combo_idx:bank_account(code,parent)";

        runDocumentQuery(joins, query -> query.addQueryHint(hintCode).addQueryHint(hintCombo));
        assertDocumentSql(sql -> {
            assertTrue(sql.contains("bank_account view bank_account_combo_idx"),
                    () -> "expected the composite index to win, got:\n" + sql);
            assertFalse(sql.contains("bank_account view bank_account_code_idx"),
                    () -> "single-column index must not win over the composite one, got:\n" + sql);
        });

        clearCapturedSql();
        runDocumentQuery(joins, query -> query.setHint(HibernateHints.HINT_COMMENT, hintCode + ";" + hintCombo));
        assertDocumentSql(sql -> {
            assertTrue(sql.contains("bank_account view bank_account_combo_idx"),
                    () -> "expected the composite index to win via HINT_COMMENT, got:\n" + sql);
            assertFalse(sql.contains("bank_account view bank_account_code_idx"),
                    () -> "single-column index must not win via HINT_COMMENT, got:\n" + sql);
        });
    }

    
    @Test
    void compositeHint_withOneColumnMissing_isNotApplied() {
        String joins = "join fetch d.accDt";
        String hintCombo = "use_index:bank_account_combo_idx:bank_account(code,parent)";

        runDocumentQuery(joins, query -> query.addQueryHint(hintCombo));
        assertDocumentSql(sql -> assertFalse(sql.contains("bank_account view"),
                () -> "partial composite hint must not inject a view, got:\n" + sql));

        clearCapturedSql();
        runDocumentQuery(joins, query -> query.setHint(HibernateHints.HINT_COMMENT, hintCombo));
        assertDocumentSql(sql -> assertFalse(sql.contains("bank_account view"),
                () -> "partial composite hint must not inject a view via HINT_COMMENT, got:\n" + sql));
    }

    private void runDocumentQuery(String joinClause, Consumer<Query<BankDocument>> configure) {
        sessionFactory.inTransaction(session -> {
            Query<BankDocument> query = session.createQuery(
                    "select d from BankDocument d " + joinClause + " where d.id = 1",
                    BankDocument.class
            );
            configure.accept(query);
            BankDocument document = query.getSingleResult();
            assertEquals(1, document.getId());
        });
    }

    private void assertDocumentSql(Consumer<String> assertion) {
        Optional<String> sql = CapturingStatementInspector.lastSqlContaining("from bank_document");
        assertTrue(sql.isPresent(), "no SELECT against bank_document was captured");
        assertion.accept(sql.get());
    }

    private static BankAccount newAccount(int id, String code, String parent, String name) {
        BankAccount account = new BankAccount();
        account.setId(id);
        account.setCode(code);
        account.setParent(parent);
        account.setName(name);
        return account;
    }

    
    public static final class CapturingStatementInspector implements StatementInspector {

        private static final List<String> CAPTURED = new ArrayList<>();

        public static synchronized void clear() {
            CAPTURED.clear();
        }

        static synchronized Optional<String> lastSqlContaining(String needle) {
            for (int i = CAPTURED.size() - 1; i >= 0; i--) {
                if (CAPTURED.get(i).contains(needle)) {
                    return Optional.of(CAPTURED.get(i));
                }
            }
            return Optional.empty();
        }

        @Override
        public synchronized String inspect(String sql) {
            CAPTURED.add(sql);
            return sql;
        }
    }
}
