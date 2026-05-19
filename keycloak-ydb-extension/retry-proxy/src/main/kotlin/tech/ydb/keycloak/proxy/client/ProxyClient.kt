package tech.ydb.keycloak.proxy.client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import tech.ydb.keycloak.proxy.config.ClientConfig

fun createProxyClient(config: ClientConfig): HttpClient = HttpClient(CIO) {
  engine {
    maxConnectionsCount = config.maxConnectionsCount
    endpoint {
      connectTimeout = config.connectTimeoutMs
      requestTimeout = config.requestTimeoutMs
    }
  }
  expectSuccess = false
  followRedirects = false
}
