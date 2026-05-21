package tech.ydb.keycloak.proxy.utils

import io.ktor.http.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.keycloak.proxy.utils.YdbRetryableBody.isRetryable503

class YdbRetryableBodyTest {

  @Test
  fun non503Status() {
    assertFalse(isRetryable503(HttpStatusCode.OK, "ydb_retryable".toByteArray()))
  }

  @Test
  fun detectsMarkerInKeycloakJson() {
    assertTrue(
      isRetryable503(
        HttpStatusCode.ServiceUnavailable,
        """{"error":"ydb_retryable","error_description":"Transaction aborted, please retry"}""".toByteArray(),
      ),
    )
  }

  @Test
  fun detectsMarkerInPlainText() {
    assertTrue(isRetryable503(HttpStatusCode.ServiceUnavailable, "ydb_retryable".toByteArray()))
    assertTrue(isRetryable503(HttpStatusCode.ServiceUnavailable, "error=ydb_retryable".toByteArray()))
  }

  @Test
  fun detectsMarkerAsSubstring() {
    assertTrue(
      isRetryable503(
        HttpStatusCode.ServiceUnavailable,
        """{"error":"internal","error_description":"ydb_retryable"}""".toByteArray(),
      ),
    )
    assertTrue(
      isRetryable503(HttpStatusCode.ServiceUnavailable, "<html>ydb_retryable</html>".toByteArray()),
    )
  }

  @Test
  fun ignoresNonMatching503Body() {
    assertFalse(isRetryable503(HttpStatusCode.ServiceUnavailable, "service unavailable".toByteArray()))
    assertFalse(isRetryable503(HttpStatusCode.ServiceUnavailable, "".toByteArray()))
  }

  @Test
  fun handlesNotOnlyStringByteArrays() {
    assertFalse(isRetryable503(HttpStatusCode.ServiceUnavailable, byteArrayOf(0x00, 0x01, 0xFF.toByte(), 0xFE.toByte())))
  }
}
