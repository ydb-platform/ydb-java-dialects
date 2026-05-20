package tech.ydb.keycloak.proxy.service

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpHeaders.Forwarded
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.slf4j.LoggerFactory
import tech.ydb.keycloak.proxy.config.ProxyConfig
import tech.ydb.keycloak.proxy.service.ProxyResult.*
import tech.ydb.keycloak.proxy.utils.isHeader
import tech.ydb.keycloak.proxy.utils.isHopByHop
import kotlin.coroutines.coroutineContext
import kotlin.random.Random
import io.ktor.http.content.ByteArrayContent as OutgoingByteArrayContent

class ProxyService(
  private val client: HttpClient,
  private val config: ProxyConfig,
) {
  private val log = LoggerFactory.getLogger(ProxyService::class.java)

  suspend fun proxyRequest(
    method: HttpMethod,
    path: String,
    body: ByteArray,
    contentType: ContentType,
    headers: Headers,
    host: String,
    remoteHost: String,
    scheme: String,
  ): ProxyResult {
    for (attempt in 0..config.maxRetries) {
      if (!coroutineContext.isActive) {
        log.info("Client disconnected, stopping retries for $method $path (attempt $attempt)")
        return ClientDisconnected
      }

      val response = try {
        forwardToTarget(method, path, body, contentType, headers, host, remoteHost, scheme)
      } catch (e: Exception) {
        return Error("Proxy error: ${e.message}")
      }

      val responseBody = response.readRawBytes()

      val isRetryable = response.status.value == 503 && String(responseBody).contains("ydb_retryable")

      if (isRetryable) {
        if (retryWithBackoff(attempt, method, path)) continue
      } else {
        return Success(responseBody, response.headers, response.contentType(), response.status)
      }
    }

    return Error("All ${config.maxRetries} retries exhausted for $method $path")
  }

  private suspend fun retryWithBackoff(attempt: Int, method: HttpMethod, path: String): Boolean {
    if (attempt >= config.maxRetries) {
      log.warn("YDB retryable 503 on $method $path, all ${config.maxRetries} retries exhausted")
      return false
    }

    backoffDelay(attempt).let { delayMs ->
      log.warn("YDB retryable 503 on $method $path (attempt ${attempt + 1}/${config.maxRetries}), retrying in ${delayMs}ms")
      delay(delayMs)
    }

    return true
  }

  private suspend fun forwardToTarget(
    method: HttpMethod,
    path: String,
    body: ByteArray,
    contentType: ContentType,
    headers: Headers,
    host: String,
    remoteHost: String,
    scheme: String,
  ): HttpResponse = client.request("${config.targetUrl}$path") {
    this.method = method

    copyHeaders(headers)
    header(Forwarded, "for=$remoteHost;host=$host;proto=$scheme")

    setBody(OutgoingByteArrayContent(body, contentType))
  }

  private fun HttpRequestBuilder.copyHeaders(headers: Headers) {
    headers.forEach { name, values ->
      if (!isHopByHop(name)
        && !isHeader(name, HttpHeaders.Host)
        && !isHeader(name, HttpHeaders.ContentType)
        && !isHeader(name, HttpHeaders.ContentLength)
      ) {
        values.forEach { header(name, it) }
      }
    }
  }

  fun backoffDelay(attempt: Int): Long {
    val exponential = (config.baseDelayMs shl attempt).coerceAtMost(config.maxDelayMs)
    return Random.nextLong(0, exponential + 1)
  }
}

sealed class ProxyResult {
  class Success(
    val body: ByteArray,
    val headers: Headers,
    val contentType: ContentType?,
    val status: HttpStatusCode,
  ) : ProxyResult()

  data class Error(val message: String) : ProxyResult()
  data object ClientDisconnected : ProxyResult()
}
