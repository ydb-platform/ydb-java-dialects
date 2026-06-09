package tech.ydb.retry;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tech.ydb.core.StatusCode.ABORTED;
import static tech.ydb.core.StatusCode.BAD_SESSION;
import static tech.ydb.core.StatusCode.SCHEME_ERROR;

/**
 * Verifies the {@link YdbTransactionInterceptor} extracts the YDB {@code StatusCode} purely from
 * {@link SQLException#getErrorCode()} traversed through the exception chain (Spring-Data /
 * application wrapping), without depending on any {@code ydb-jdbc-driver} type at runtime.
 */
class SqlExceptionStatusExtractionTest extends InterceptorTestSupport {

    @Test
    void shouldRetryWhenSqlExceptionDirectlyCarriesRetryableStatus() throws Throwable {
        TestableInterceptor interceptor = interceptorWithConfig(true, 3, 0, 0, 0, 0);
        interceptor.enqueueOutcome(plainSqlException(BAD_SESSION.getCode()), "ok");

        Object result = interceptor.invoke(invocationFor("regularTx"));

        assertEquals("ok", result);
        assertEquals(2, interceptor.allInvocations());
    }

    @Test
    void shouldRetryWhenSqlExceptionIsBuriedInSpringDataAccessWrapper() throws Throwable {
        TestableInterceptor interceptor = interceptorWithConfig(true, 3, 0, 0, 0, 0);
        DataIntegrityViolationException wrapper = new DataIntegrityViolationException(
                "wrapped", plainSqlException(ABORTED.getCode()));
        interceptor.enqueueOutcome(wrapper, "ok");

        Object result = interceptor.invoke(invocationFor("regularTx"));

        assertEquals("ok", result);
        assertEquals(2, interceptor.allInvocations());
    }

    @Test
    void shouldNotRetryWhenSqlExceptionHasNonRetryableStatus() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(plainSqlException(SCHEME_ERROR.getCode()));

        SQLException thrown =
                assertThrows(SQLException.class, () -> interceptor.invoke(invocationFor("regularTx")));

        assertEquals(SCHEME_ERROR.getCode(), thrown.getErrorCode());
        assertEquals(1, interceptor.allInvocations());
    }

    @Test
    void shouldNotRetryWhenSqlExceptionHasZeroVendorCode() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new SQLException("non-ydb driver", null, 0));

        assertThrows(SQLException.class, () -> interceptor.invoke(invocationFor("regularTx")));
        assertEquals(1, interceptor.allInvocations());
    }

    @Test
    void shouldNotRetryWhenSqlExceptionHasUnknownVendorCode() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new SQLException("some-other-driver", null, 12345));

        assertThrows(SQLException.class, () -> interceptor.invoke(invocationFor("regularTx")));
        assertEquals(1, interceptor.allInvocations());
    }

    private static SQLException plainSqlException(int vendorCode) {
        return new SQLException("ydb-like failure", null, vendorCode);
    }
}
