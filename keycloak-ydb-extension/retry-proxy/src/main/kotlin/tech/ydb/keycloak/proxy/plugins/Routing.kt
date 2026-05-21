package tech.ydb.keycloak.proxy.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.routing.*
import tech.ydb.keycloak.proxy.Dependencies

fun Application.configureRouting(deps: Dependencies) {
  install(ForwardedHeaders)

  routing {
    route("{...}") {
      handle {
        deps.controller.handle(call)
      }
    }
  }
}
