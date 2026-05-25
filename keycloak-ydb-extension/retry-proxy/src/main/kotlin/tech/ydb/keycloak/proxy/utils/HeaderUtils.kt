package tech.ydb.keycloak.proxy.utils

import io.ktor.http.*

// https://datatracker.ietf.org/doc/html/rfc2616#section-13.5.1
val HOP_BY_HOP_HEADERS = listOf(
  HttpHeaders.Connection,
  "Keep-Alive",
  HttpHeaders.ProxyAuthenticate,
  HttpHeaders.ProxyAuthorization,
  HttpHeaders.TE,
  HttpHeaders.Trailer,
  HttpHeaders.TransferEncoding,
  HttpHeaders.Upgrade,
)

fun isHopByHop(name: String): Boolean = HOP_BY_HOP_HEADERS.any { it.equals(name, ignoreCase = true) }

fun isHeader(name: String, header: String): Boolean = name.equals(header, ignoreCase = true)
