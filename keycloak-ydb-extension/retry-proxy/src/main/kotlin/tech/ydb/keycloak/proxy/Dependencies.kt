package tech.ydb.keycloak.proxy

import io.ktor.client.*
import tech.ydb.keycloak.proxy.client.createProxyClient
import tech.ydb.keycloak.proxy.config.ProxyConfig
import tech.ydb.keycloak.proxy.controller.ProxyController
import tech.ydb.keycloak.proxy.service.ProxyService

class Dependencies(
  val config: ProxyConfig = ProxyConfig.fromEnv(),
  val client: HttpClient = createProxyClient(config.client),
  val proxyService: ProxyService = ProxyService(client, config),
  val controller: ProxyController = ProxyController(proxyService, config),
)
