package tech.ydb.keycloak.utils

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.keycloak.testsupport.YdbRetryableExceptionUtil

class YdbErrorUtilsTest {

  @Test
  fun returnsTrueForYdbRetryableException() {
    assertTrue(isYdbRetryable(YdbRetryableExceptionUtil.ydbRetryableException()))
  }

  @Test
  fun returnsTrueWhenYdbRetryableExceptionIsCause() {
    val retryable = YdbRetryableExceptionUtil.ydbRetryableException()
    val wrapped = RuntimeException("wrapper", retryable)
    assertTrue(isYdbRetryable(wrapped))
  }

  @Test
  fun returnsTrueForDeepCauseChain() {
    val retryable = YdbRetryableExceptionUtil.ydbRetryableException()
    val wrapped = IllegalStateException("outer", RuntimeException("middle", retryable))
    assertTrue(isYdbRetryable(wrapped))
  }

  @Test
  fun returnsFalseForUnrelatedException() {
    assertFalse(isYdbRetryable(RuntimeException("fail")))
  }

  @Test
  fun returnsFalseForExceptionWithoutRetryableCause() {
    assertFalse(isYdbRetryable(RuntimeException("fail", IllegalStateException("inner"))))
  }
}
