package tech.ydb.keycloak.proxy

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory
import tech.ydb.keycloak.proxy.plugins.configureRouting

private val log = LoggerFactory.getLogger("RetryProxy")

fun main() {
  val deps = Dependencies()
  val config = deps.config

  log.info("Starting retry proxy: listen=:${config.listenPort} target=${config.targetUrl} maxRetries=${config.maxRetries} baseDelay=${config.baseDelayMs}ms maxDelay=${config.maxDelayMs}ms")

  embeddedServer(Netty, port = config.listenPort) {
    configureRouting(deps)
  }.start(wait = true)
}
