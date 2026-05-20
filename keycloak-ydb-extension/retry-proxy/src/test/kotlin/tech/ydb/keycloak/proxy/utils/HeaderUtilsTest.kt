package tech.ydb.keycloak.proxy.utils

import io.ktor.http.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HeaderUtilsTest {

  @Test
  fun hopByHopHeadersAreDetected() {
    assertTrue(isHopByHop("Connection"))
    assertTrue(isHopByHop("Transfer-Encoding"))
    assertTrue(isHopByHop("Keep-Alive"))
    assertTrue(isHopByHop("Proxy-Authenticate"))
    assertTrue(isHopByHop("Proxy-Authorization"))
    assertTrue(isHopByHop("TE"))
    assertTrue(isHopByHop("Trailer"))
    assertTrue(isHopByHop("Upgrade"))
  }

  @Test
  fun regularHeadersAreNotHopByHop() {
    assertFalse(isHopByHop("Content-Type"))
    assertFalse(isHopByHop("Authorization"))
    assertFalse(isHopByHop("Accept"))
    assertFalse(isHopByHop("Location"))
  }

  @Test
  fun hopByHopIsCaseInsensitive() {
    assertTrue(isHopByHop("connection"))
    assertTrue(isHopByHop("CONNECTION"))
    assertTrue(isHopByHop("transfer-encoding"))
    assertTrue(isHopByHop("KEEP-ALIVE"))
  }

  @Test
  fun isHeaderMatchesCaseInsensitive() {
    assertTrue(isHeader("Content-Type", HttpHeaders.ContentType))
    assertTrue(isHeader("content-type", HttpHeaders.ContentType))
    assertTrue(isHeader("CONTENT-TYPE", HttpHeaders.ContentType))
  }

  @Test
  fun isHeaderRejectsNonMatching() {
    assertFalse(isHeader("Content-Type", HttpHeaders.ContentLength))
    assertFalse(isHeader("Accept", HttpHeaders.Location))
  }
}
