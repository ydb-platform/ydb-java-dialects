package tech.ydb.keycloak.proxy.controller

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import tech.ydb.keycloak.proxy.config.ProxyConfig
import tech.ydb.keycloak.proxy.service.ProxyResult.*
import tech.ydb.keycloak.proxy.service.ProxyService
import tech.ydb.keycloak.proxy.utils.isHeader
import tech.ydb.keycloak.proxy.utils.isHopByHop

class ProxyController(
  private val proxyService: ProxyService,
  private val config: ProxyConfig,
) {
  suspend fun handle(call: ApplicationCall) {
    val method = call.request.httpMethod
    val path = call.request.uri
    val body = call.receive<ByteArray>()
    val contentType = call.request.contentType()
    val host = call.request.headers["Host"] ?: call.request.host()
    val remoteHost = call.request.local.remoteHost
    val scheme = call.request.local.scheme

    val result = proxyService.proxyRequest(
      method = method,
      path = path,
      body = body,
      contentType = contentType,
      headers = call.request.headers,
      host = host,
      remoteHost = remoteHost,
      scheme = scheme
    )

    when (result) {
      is Success -> call.handleSuccess(result)

      is Error -> call.respondText(result.message, status = HttpStatusCode.BadGateway)

      is ClientDisconnected -> {}
    }
  }

  private suspend fun ApplicationCall.handleSuccess(result: Success) {
    val originalHost = request.headers["Host"] ?: "localhost:${config.listenPort}"

    result.headers.forEach { name, values ->
      if (!isHopByHop(name) && !isHeader(name, HttpHeaders.ContentLength)) {
        values.forEach { value ->
          response.header(name, rewriteInternalUrl(name, value, originalHost))
        }
      }
    }

    respondBytes(result.body, result.contentType, result.status)
  }

  private fun rewriteInternalUrl(name: String, value: String, originalHost: String): String {
    if (isHeader(name, HttpHeaders.Location)) {
      return value.replace(config.targetUrl, "http://$originalHost")
    }
    return value
  }
}
