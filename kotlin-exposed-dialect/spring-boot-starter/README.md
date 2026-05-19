# Kotlin Exposed YDB Dialect Spring Boot Starter

Optional Spring Boot integration for [`kotlin-exposed-ydb-dialect`](../).

This module reuses Exposed's official Spring Boot starter and adds the YDB-specific pieces
that the generic starter does not know about:

- registering the YDB JDBC dialect in Exposed;
- aligning `spring.datasource.url` with `forceSignedDatetimes=...`;
- supplying the recommended `DatabaseConfig` for YDB;
- exposing `YdbTransactionOperations` for retry-aware Exposed transactions.

## Dependency

```xml
<dependency>
    <groupId>tech.ydb.dialects</groupId>
    <artifactId>kotlin-exposed-ydb-dialect-spring-boot-starter</artifactId>
    <version>0.9.0</version>
</dependency>
```

The starter pulls in:

- `tech.ydb.dialects:kotlin-exposed-ydb-dialect`
- `org.jetbrains.exposed:exposed-spring-boot-starter`
- `tech.ydb.jdbc:ydb-jdbc-driver`

## Minimal configuration

```yaml
spring:
  datasource:
    url: jdbc:ydb:grpc://localhost:2136/local
  exposed:
    ydb:
      enable-signed-datetimes: false
```

## Retry-aware transactions

`@Transactional` gives you Spring-managed Exposed transactions, but it does not add YDB retry
policy for OCC conflicts. Use `YdbTransactionOperations` when you need the retrying path:

```kotlin
@Service
class ProductService(
    private val ydbTx: YdbTransactionOperations
) {
    fun save() = ydbTx.execute {
        // Exposed DSL
    }
}
```
