package tech.ydb.keycloak.proxy.controller

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tech.ydb.keycloak.proxy.Dependencies
import tech.ydb.keycloak.proxy.config.ClientConfig
import tech.ydb.keycloak.proxy.config.ProxyConfig
import tech.ydb.keycloak.proxy.plugins.configureRouting
import tech.ydb.keycloak.proxy.service.ProxyResult.*
import tech.ydb.keycloak.proxy.service.ProxyService

class ProxyControllerTest {

  private val config = ProxyConfig(
    targetUrl = "http://backend:8080",
    maxRetries = 3,
    baseDelayMs = 10,
    maxDelayMs = 100,
    listenPort = 9090,
    client = ClientConfig(
      maxConnectionsCount = 10,
      connectTimeoutMs = 1000,
      requestTimeoutMs = 5000,
    ),
  )

  private val proxyService = mockk<ProxyService>()

  private fun withProxy(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
    application {
      configureRouting(
        Dependencies(
          config = config,
          client = mockk(),
          proxyService = proxyService,
          controller = ProxyController(proxyService, config),
        )
      )
    }

    block()
  }

  @Test
  fun forwardsSuccessResponse() = withProxy {
    coEvery { proxyService.proxyRequest(any(), any(), any(), any(), any(), any(), any(), any()) } returns
      Success(
        body = "hello".toByteArray(),
        headers = headersOf(),
        contentType = ContentType.Text.Plain,
        status = HttpStatusCode.OK,
      )

    client.get("/test").let {
      assertEquals(HttpStatusCode.OK, it.status)
      assertEquals("hello", it.bodyAsText())
    }
  }

  @Test
  fun returnsErrorAsBadGateway() = withProxy {
    coEvery { proxyService.proxyRequest(any(), any(), any(), any(), any(), any(), any(), any()) } returns
      Error("connection refused")

    client.get("/fail").let {
      assertEquals(HttpStatusCode.BadGateway, it.status)
      assertEquals("connection refused", it.bodyAsText())
    }
  }

  @Test
  fun filtersHopByHopHeaders() = withProxy {
    coEvery { proxyService.proxyRequest(any(), any(), any(), any(), any(), any(), any(), any()) } returns
      Success(
        body = "ok".toByteArray(),
        headers = headersOf(
          HttpHeaders.Connection to listOf("keep-alive"),
          HttpHeaders.TransferEncoding to listOf("chunked"),
          HttpHeaders.ContentLength to listOf("999"),
          "X-Custom" to listOf("value"),
        ),
        contentType = ContentType.Text.Plain,
        status = HttpStatusCode.OK,
      )

    client.get("/headers").let {
      assertEquals("value", it.headers["X-Custom"])
      assertNull(it.headers[HttpHeaders.Connection])
      assertNull(it.headers[HttpHeaders.TransferEncoding])
      // Backend's Content-Length (999) must not leak — Ktor sets its own based on actual body size
      assertEquals("2", it.headers[HttpHeaders.ContentLength])
    }
  }

  @Test
  fun rewritesLocationHeader() = withProxy {
    coEvery { proxyService.proxyRequest(any(), any(), any(), any(), any(), any(), any(), any()) } returns
      Success(
        body = ByteArray(0),
        headers = headersOf(HttpHeaders.Location, "http://backend:8080/realms/master"),
        contentType = null,
        status = HttpStatusCode.Found,
      )

    createClient { followRedirects = false }.get("/login").let {
      assertEquals(HttpStatusCode.Found, it.status)
      assertTrue(it.headers[HttpHeaders.Location]!!.contains("/realms/master"))
      assertFalse(it.headers[HttpHeaders.Location]!!.contains("backend:8080"))
    }
  }

  @Test
  fun clientDisconnectedReturnsNothing() = withProxy {
    coEvery { proxyService.proxyRequest(any(), any(), any(), any(), any(), any(), any(), any()) } returns
      ClientDisconnected

    client.get("/disconnect").let {
      assertEquals("", it.bodyAsText())
    }
  }
}
