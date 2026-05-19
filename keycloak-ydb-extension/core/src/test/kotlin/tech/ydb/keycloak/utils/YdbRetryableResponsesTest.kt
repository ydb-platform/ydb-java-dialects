package tech.ydb.keycloak.utils

import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.keycloak.testsupport.YdbRetryableExceptionUtil

class YdbRetryableResponsesTest {

  @Test
  fun build503SetsExpectedHeadersAndBody() {
    val cause = YdbRetryableExceptionUtil.ydbRetryableException()
    val response = YdbRetryableResponses.build503(cause, YdbRetryableResponses.CONTENTION_DESCRIPTION)

    assertEquals(Response.Status.SERVICE_UNAVAILABLE.statusCode, response.status)
    assertEquals("application", response.mediaType?.type)
    assertEquals("json", response.mediaType?.subtype)
    val body = response.entity as String
    assertTrue(body.contains("\"error\":\"${YdbRetryableResponses.ERROR_CODE}\""))
    assertTrue(body.contains(YdbRetryableResponses.CONTENTION_DESCRIPTION))
  }
}
