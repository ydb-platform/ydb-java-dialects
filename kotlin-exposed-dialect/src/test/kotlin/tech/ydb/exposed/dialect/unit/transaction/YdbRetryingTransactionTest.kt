//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.Test
//import tech.ydb.exposed.dialect.transaction.ydbTransaction
//import java.sql.SQLException
//import java.util.concurrent.atomic.AtomicInteger
//
//class YdbRetryingTransactionTest {
//
//    @Test
//    fun `should retry aborted transaction`() {
//        val attempts = AtomicInteger(0)
//
//        val result = ydbTransaction(maxRetries = 3) {
//            if (attempts.incrementAndGet() < 3) {
//                throw SQLException("ABORTED")
//            }
//            42
//        }
//
//        assertEquals(42, result)
//        assertEquals(3, attempts.get())
//    }
//
//    @Test
//    fun `should not retry non retryable errors`() {
//        val attempts = AtomicInteger(0)
//
//        val exception = runCatching {
//            ydbTransaction(maxRetries = 3) {
//                attempts.incrementAndGet()
//                throw SQLException("SYNTAX ERROR")
//            }
//        }.exceptionOrNull()
//
//        assertEquals(1, attempts.get())
//        assertTrue(exception is SQLException)
//    }
//}