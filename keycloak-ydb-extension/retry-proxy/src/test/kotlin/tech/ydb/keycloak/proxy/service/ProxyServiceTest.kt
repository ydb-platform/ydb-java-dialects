package tech.ydb.keycloak.proxy.service

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tech.ydb.keycloak.proxy.config.ProxyConfig

class ProxyServiceTest {

  private val config = mockk<ProxyConfig> {
    every { targetUrl } returns "http://backend:8080"
    every { maxRetries } returns 3
    every { baseDelayMs } returns 1L
    every { maxDelayMs } returns 10L
    every { listenPort } returns 9090
  }

  private fun mockClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient {
    return HttpClient(MockEngine) {
      engine { addHandler(handler) }
      expectSuccess = false
      followRedirects = false
    }
  }

  private fun proxyService(client: HttpClient) = ProxyService(client, config)

  private suspend fun doRequest(service: ProxyService): ProxyResult = service.proxyRequest(
    method = HttpMethod.Get,
    path = "/test",
    body = ByteArray(0),
    contentType = ContentType.Application.Json,
    headers = headersOf(),
    host = "localhost:9090",
    remoteHost = "127.0.0.1",
    scheme = "http",
  )

  @Test
  fun forwardsSuccessfulResponse() = runTest {
    val service = proxyService(mockClient {
      respond("ok", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/plain"))
    })

    doRequest(service).let {
      assertInstanceOf(ProxyResult.Success::class.java, it)
      it as ProxyResult.Success
      assertEquals(HttpStatusCode.OK, it.status)
      assertEquals("ok", String(it.body))
    }
  }

  @Test
  fun returnsErrorOnException() = runTest {
    val service = proxyService(mockClient {
      throw RuntimeException("connection refused")
    })

    doRequest(service).let {
      assertInstanceOf(ProxyResult.Error::class.java, it)
      it as ProxyResult.Error
      assertEquals("Proxy error: connection refused", it.message)
    }
  }

  @Test
  fun doesNotRetryOnException() = runTest {
    var requestCount = 0
    val service = proxyService(mockClient {
      requestCount++
      throw RuntimeException("fail")
    })

    doRequest(service)
    assertEquals(1, requestCount)
  }

  @Test
  fun retriesOnYdbRetryable503() = runTest {
    var requestCount = 0
    val service = proxyService(mockClient {
      requestCount++
      if (requestCount <= 2) {
        respond("""{"error": "ydb_retryable"}""", HttpStatusCode.ServiceUnavailable)
      } else {
        respond("ok", HttpStatusCode.OK)
      }
    })

    doRequest(service).let {
      assertInstanceOf(ProxyResult.Success::class.java, it)
      it as ProxyResult.Success
      assertEquals(HttpStatusCode.OK, it.status)
      assertEquals("ok", String(it.body))
    }
    assertEquals(3, requestCount)
  }

  @Test
  fun returnsErrorWhenRetriesExhausted() = runTest {
    every { config.maxRetries } returns 2
    var requestCount = 0
    val service = proxyService(mockClient {
      requestCount++
      respond("""{"error": "ydb_retryable"}""", HttpStatusCode.ServiceUnavailable)
    })

    doRequest(service).let {
      assertInstanceOf(ProxyResult.Error::class.java, it)
      it as ProxyResult.Error
      assertTrue(it.message.contains("retries exhausted"))
    }
    // 1 initial + 2 retries = 3
    assertEquals(3, requestCount)

    every { config.maxRetries } returns 3
  }

  @Test
  fun doesNotRetryOnNonRetryable503() = runTest {
    var requestCount = 0
    val service = proxyService(mockClient {
      requestCount++
      respond("service unavailable", HttpStatusCode.ServiceUnavailable)
    })

    doRequest(service).let {
      assertInstanceOf(ProxyResult.Success::class.java, it)
      it as ProxyResult.Success
      assertEquals(HttpStatusCode.ServiceUnavailable, it.status)
    }
    assertEquals(1, requestCount)
  }

  @Test
  fun forwardsNon503ErrorStatusAsIs() = runTest {
    val service = proxyService(mockClient {
      respond("not found", HttpStatusCode.NotFound)
    })

    doRequest(service).let {
      assertInstanceOf(ProxyResult.Success::class.java, it)
      it as ProxyResult.Success
      assertEquals(HttpStatusCode.NotFound, it.status)
      assertEquals("not found", String(it.body))
    }
  }

  @Test
  fun forwardsRequestToCorrectUrl() = runTest {
    var capturedUrl: String? = null
    val service = proxyService(mockClient { request ->
      capturedUrl = request.url.toString()
      respond("ok", HttpStatusCode.OK)
    })

    doRequest(service)
    assertEquals("http://backend:8080/test", capturedUrl)
  }

  @Test
  fun setsForwardedHeader() = runTest {
    var capturedHeaders: Headers? = null
    val service = proxyService(mockClient { request ->
      capturedHeaders = request.headers
      respond("ok", HttpStatusCode.OK)
    })

    doRequest(service)
    assertEquals("for=127.0.0.1;host=localhost:9090;proto=http", capturedHeaders!![HttpHeaders.Forwarded])
  }

  @Test
  fun filtersHopByHopAndServiceHeaders() = runTest {
    var capturedHeaders: Headers? = null
    val service = proxyService(mockClient { request ->
      capturedHeaders = request.headers
      respond("ok", HttpStatusCode.OK)
    })

    service.proxyRequest(
      method = HttpMethod.Get,
      path = "/test",
      body = ByteArray(0),
      contentType = ContentType.Application.Json,
      headers = headersOf(
        HttpHeaders.Connection to listOf("keep-alive"),
        HttpHeaders.Host to listOf("original-host"),
        HttpHeaders.ContentType to listOf("application/json"),
        HttpHeaders.ContentLength to listOf("0"),
        "X-Custom" to listOf("value"),
        HttpHeaders.Authorization to listOf("Bearer token"),
      ),
      host = "localhost:9090",
      remoteHost = "127.0.0.1",
      scheme = "http",
    )

    assertNull(capturedHeaders!![HttpHeaders.Connection])
    assertNull(capturedHeaders!![HttpHeaders.Host])
    assertNull(capturedHeaders!![HttpHeaders.ContentType])
    assertNull(capturedHeaders!![HttpHeaders.ContentLength])
    assertEquals("value", capturedHeaders!!["X-Custom"])
    assertEquals("Bearer token", capturedHeaders!![HttpHeaders.Authorization])
  }

  @Test
  fun backoffDelayIsWithinBounds() {
    val service = proxyService(mockClient { respond("ok", HttpStatusCode.OK) })

    repeat(100) {
      val delay = service.backoffDelay(0)
      assertTrue(delay in 0..config.baseDelayMs)
    }

    repeat(100) {
      val delay = service.backoffDelay(5)
      assertTrue(delay in 0..config.maxDelayMs)
    }
  }
}
