# Kotlin Exposed YDB Dialect Spring Boot Starter

Optional Spring Boot integration for [`kotlin-exposed-ydb-dialect`](../).

This module reuses Exposed's official Spring Boot starter and adds the YDB-specific pieces
that the generic starter does not know about:

- registering the YDB JDBC dialect in Exposed;
- aligning `spring.datasource.url` with `forceSignedDatetimes=...`;
- defaulting `spring.datasource.driver-class-name` to `tech.ydb.jdbc.YdbDriver` when it is omitted;
- supplying the recommended `DatabaseConfig` for YDB as the primary Spring bean;
- creating an Exposed `Database` bean from the Spring-managed `DataSource`;
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

If `spring.datasource.driver-class-name` is not specified, the starter sets it to
`tech.ydb.jdbc.YdbDriver` automatically.

When `spring.exposed.ydb.enable-signed-datetimes=true`, the starter also propagates the
matching `forceSignedDatetimes=true` flag into the normalized JDBC URL.

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
