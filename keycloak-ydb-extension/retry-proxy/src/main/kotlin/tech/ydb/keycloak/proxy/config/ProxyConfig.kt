package tech.ydb.keycloak.proxy.config

data class ClientConfig(
  val maxConnectionsCount: Int,
  val connectTimeoutMs: Long,
  val requestTimeoutMs: Long,
) {
  companion object {
    fun fromEnv(): ClientConfig = ClientConfig(
      maxConnectionsCount = (System.getenv("CLIENT_MAX_CONNECTIONS") ?: "1000").toInt(),
      connectTimeoutMs = (System.getenv("CLIENT_CONNECT_TIMEOUT_MS") ?: "10000").toLong(),
      requestTimeoutMs = (System.getenv("CLIENT_REQUEST_TIMEOUT_MS") ?: "30000").toLong(),
    )
  }
}

data class ProxyConfig(
  val targetUrl: String,
  val maxRetries: Int,
  val baseDelayMs: Long,
  val maxDelayMs: Long,
  val listenPort: Int,
  val client: ClientConfig,
) {
  companion object {
    fun fromEnv(): ProxyConfig = ProxyConfig(
      targetUrl = System.getenv("TARGET_URL") ?: "http://localhost:8080",
      maxRetries = (System.getenv("MAX_RETRIES") ?: "10").toInt(),
      baseDelayMs = (System.getenv("BASE_DELAY_MS") ?: "50").toLong(),
      maxDelayMs = (System.getenv("MAX_DELAY_MS") ?: "2000").toLong(),
      listenPort = (System.getenv("LISTEN_PORT") ?: "8080").toInt(),
      client = ClientConfig.fromEnv(),
    )
  }
}
