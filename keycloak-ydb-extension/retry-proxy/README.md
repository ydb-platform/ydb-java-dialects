# YDB Retry Proxy

HTTP reverse proxy for Keycloak that automatically retries requests on YDB-specific retryable errors.

## Overview

The proxy sits between the client and Keycloak, intercepting all HTTP traffic. When the backend returns a `503` response
with a body containing `ydb_retryable`, the proxy transparently retries the request using exponential backoff with
jitter.

Key behaviors:

- Retries only on `503` responses containing `ydb_retryable` in the body
- Exponential backoff with jitter: `baseDelay * 2^attempt`, capped at `maxDelay`
- Stops retrying if the client disconnects
- Returns `502 Bad Gateway` on connection errors (timeout, DNS, IOException) without retrying
- Filters hop-by-hop headers (RFC 2616 Section 13.5.1)
- Rewrites `Location` headers from the internal target address to the proxy address

## Configuration

All parameters are set via environment variables.

### Proxy

| Variable        | Default                 | Description                                   |
|-----------------|-------------------------|-----------------------------------------------|
| `TARGET_URL`    | `http://localhost:8080` | URL of the target server (Keycloak)           |
| `LISTEN_PORT`   | `8080`                  | Port the proxy listens on                     |
| `MAX_RETRIES`   | `10`                    | Maximum number of retries on retryable errors |
| `BASE_DELAY_MS` | `50`                    | Base delay before the first retry (ms)        |
| `MAX_DELAY_MS`  | `2000`                  | Maximum delay between retries (ms)            |

### Keycloak configuration

When Keycloak is deployed behind this proxy, its `hostname` setting must match the public address the proxy is exposed on — not `localhost`. Otherwise Keycloak generates login form URLs pointing to a different origin than the client connected to, which breaks the session cookie flow (`cookie_not_found`).

Example (`keycloak.conf`):

```properties
proxy-headers=forwarded
hostname=http://0.0.0.0:9090   # must match the address clients use to reach the proxy
```

### HTTP Client

| Variable                    | Default | Description                              |
|-----------------------------|---------|------------------------------------------|
| `CLIENT_MAX_CONNECTIONS`    | `1000`  | Maximum number of concurrent connections |
| `CLIENT_CONNECT_TIMEOUT_MS` | `10000` | Connection establishment timeout (ms)    |
| `CLIENT_REQUEST_TIMEOUT_MS` | `30000` | Response wait timeout (ms)               |
