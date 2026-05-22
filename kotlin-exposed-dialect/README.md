# Kotlin Exposed YDB Dialect

YDB integration for [JetBrains Exposed](https://github.com/JetBrains/Exposed) via JDBC.

The module provides:

- a Kotlin Exposed `VendorDialect` for YDB;
- `createYdbStatement()` for YDB-compatible `CREATE TABLE` rendering;
- native `UPSERT` / `REPLACE` support through Exposed's `Table.upsert` and `Table.replace`;
- retry-aware `ydbTransaction { ... }` for YDB OCC conflicts;
- YDB-specific column types for temporal, JSON, interval, decimal, UUID, and unsigned values.

## Requirements

- JDK 17+
- Maven
- [YDB JDBC driver](https://github.com/ydb-platform/ydb-jdbc-driver) on the application classpath
- JetBrains Exposed 1.3.x

```xml
<dependency>
    <groupId>tech.ydb.jdbc</groupId>
    <artifactId>ydb-jdbc-driver</artifactId>
    <version><!-- align with your YDB deployment --></version>
</dependency>
<dependency>
    <groupId>tech.ydb.dialects</groupId>
    <artifactId>kotlin-exposed-ydb-dialect</artifactId>
    <version>0.9.0</version>
</dependency>
```

## Quick start

```kotlin
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import tech.ydb.exposed.dialect.registerYdbDialect
import tech.ydb.exposed.dialect.ydbTransaction
import java.sql.Connection

registerYdbDialect()

val db = Database.connect(
    url = "jdbc:ydb:grpc://localhost:2136/local",
    driver = "tech.ydb.jdbc.YdbDriver",
    databaseConfig = DatabaseConfig {
        defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        defaultReadOnly = false
        useNestedTransactions = false
    }
)

ydbTransaction(db) {
    // Exposed DSL / DAO code
}
```

These `DatabaseConfig` defaults match the tested YDB connection recipe used by the
integration tests and mirror the behavior that the former Spring Boot starter applied automatically.

## Spring Boot 3

No dedicated Spring Boot starter is shipped for the dialect. For Spring Boot 3,
use Exposed's official Spring integration and add a small YDB-specific configuration
class in the application itself.

Add the usual Exposed Spring dependency together with the dialect and the YDB JDBC driver:

```xml
<dependency>
    <groupId>org.jetbrains.exposed</groupId>
    <artifactId>exposed-spring-boot-starter</artifactId>
    <version>${exposed.version}</version>
</dependency>
<dependency>
    <groupId>tech.ydb.dialects</groupId>
    <artifactId>kotlin-exposed-ydb-dialect</artifactId>
    <version>0.9.0</version>
</dependency>
<dependency>
    <groupId>tech.ydb.jdbc</groupId>
    <artifactId>ydb-jdbc-driver</artifactId>
    <version><!-- align with your YDB deployment --></version>
</dependency>
```

Example application properties:

```yaml
spring:
  datasource:
    url: jdbc:ydb:grpc://localhost:2136/local?forceSignedDatetimes=false
    driver-class-name: tech.ydb.jdbc.YdbDriver
  exposed:
    generate-ddl: false
    show-sql: false
    ydb:
      enable-signed-datetimes: false
```

In this setup, `spring.exposed.ydb.enable-signed-datetimes` is an application-level
property used by the custom configuration below. If signed temporal mode is enabled,
keep it aligned with the JDBC URL by setting `forceSignedDatetimes=true` as well.
If the URL already contains query parameters, append it as `&forceSignedDatetimes=true`.

Example configuration:

```kotlin
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import tech.ydb.exposed.dialect.registerYdbDialect
import java.sql.Connection
import javax.sql.DataSource

@Configuration
class YdbExposedConfiguration(
    @Value("\${spring.exposed.ydb.enable-signed-datetimes:false}")
    private val enableSignedDatetimes: Boolean
) {
    @Bean
    fun ydbDialectRegistration(): InitializingBean = InitializingBean {
        registerYdbDialect(enableSignedDatetimes = enableSignedDatetimes)
    }

    @Bean
    fun databaseConfig(): DatabaseConfig = DatabaseConfig {
        defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        defaultReadOnly = false
        useNestedTransactions = false
    }

    @Bean
    @DependsOn("ydbDialectRegistration")
    fun database(dataSource: DataSource, databaseConfig: DatabaseConfig): Database =
        Database.connect(
            datasource = dataSource,
            databaseConfig = databaseConfig
        )
}
```

This keeps the Spring setup close to the official Exposed guide while making the YDB-specific
parts explicit in the application:

- register the dialect through `registerYdbDialect(...)`;
- set `tech.ydb.jdbc.YdbDriver` explicitly;
- keep `forceSignedDatetimes=...` in the JDBC URL aligned with the dialect mode;
- create the Exposed `Database` from the Spring-managed `DataSource`;
- use the YDB-oriented `DatabaseConfig` defaults shown above.

That is the same logic the former dedicated starter used to automate, now expressed as
application-level configuration instead of a separate published module.

For retry-aware YDB transactions inside Spring services, inject `Database` and call
`ydbTransaction(database) { ... }` directly, or wrap that call in your own helper component.

## Defining tables

YDB requires a table-level `PRIMARY KEY (...)` in `CREATE TABLE`, not the inline
`column Type PRIMARY KEY` form that Exposed may generate for a single-column PK.

Because Exposed 1.3.0 does not expose a dialect hook for this part of `CREATE TABLE`,
YDB schema generation is implemented as a local workaround: override `createStatement()`
and delegate to `createYdbStatement()`.

```kotlin
import org.jetbrains.exposed.v1.core.Table
import tech.ydb.exposed.dialect.createYdbStatement
import tech.ydb.exposed.dialect.javatime.ydbTimestamp64
import tech.ydb.exposed.dialect.ydbDecimal

object Products : Table("products") {
    val id = integer("id")
    val sku = varchar("sku", 64)
    val name = varchar("name", 255)
    val category = varchar("category", 128)
    val price = ydbDecimal("price", precision = 10, scale = 2)
    val expiresAt = ydbTimestamp64("expires_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, sku)
    }

    override fun createStatement(): List<String> = createYdbStatement()
}
```

`createYdbStatement()`:

- renders all columns without inline PK declarations;
- appends a table-level `PRIMARY KEY (...)`;
- preserves `NOT NULL` and `DEFAULT`;
- preserves `storageParameters`, so YDB-specific `WITH (...)` clauses can still be used.

Post-create indexes declared through `Table.index(...)` are still emitted through the dialect's
standard `ALTER TABLE ... ADD INDEX ... GLOBAL` path.

### TTL via storage parameters

If you need YDB-specific table options such as TTL, declare them through Exposed
`storageParameters` and keep the DDL override:

```kotlin
import org.jetbrains.exposed.v1.core.RawTableStorageParameter
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TableStorageParameter
import tech.ydb.exposed.dialect.createYdbStatement
import tech.ydb.exposed.dialect.javatime.ydbTimestamp64

object Sessions : Table("sessions") {
    val id = integer("id")
    val expireAt = ydbTimestamp64("expire_at")

    override val primaryKey = PrimaryKey(id)

    override val storageParameters: List<TableStorageParameter> =
        listOf(RawTableStorageParameter("TTL = Interval(\"PT1H\") ON expire_at"))

    override fun createStatement(): List<String> = createYdbStatement()
}
```

## Insert / upsert / replace

Exposed's regular DSL works as-is. The dialect also maps Exposed's `upsert` / `replace`
to native YDB `UPSERT` / `REPLACE`.

```kotlin
Products.upsert {
    it[id] = 1
    it[sku] = "BOOK-001"
    it[name] = "Kotlin in Action"
    it[category] = "books"
    it[price] = BigDecimal("39.90")
}

Products.replace {
    it[id] = 1
    it[sku] = "BOOK-001"
    it[name] = "Kotlin in Action, 2nd edition"
    it[category] = "books"
    it[price] = BigDecimal("44.90")
}
```

Behavioral notes:

- `UPSERT` writes only the columns listed in the statement;
- on PK conflict, columns omitted from `UPSERT` remain unchanged;
- when inserting a new row, every required `NOT NULL` column without a default must be initialized;
- `REPLACE` overwrites the row by PK, so omitted columns are reset to defaults;
- if a required column has no default, omitting it from `REPLACE` yields an error;
- `upsert(where)` is not supported;
- ANSI `MERGE` is intentionally rejected.

## Retryable transactions

YDB uses Optimistic Concurrency Control, so a transaction can fail with a retryable status.
Use `ydbTransaction` instead of plain `transaction` when you want retries on retryable YDB errors.

```kotlin
import tech.ydb.exposed.dialect.YdbRetryConfig
import tech.ydb.exposed.dialect.ydbTransaction

ydbTransaction(db) {
    // read-write transaction
}

ydbTransaction(db, retry = YdbRetryConfig.IDEMPOTENT) {
    // safe-to-repeat body
}

ydbTransaction(db, readOnly = true, retry = YdbRetryConfig.IDEMPOTENT) {
    // read-only transaction
}
```

`YdbRetryConfig.IDEMPOTENT` should only be used when the body can be safely executed more than once.

## Types

Default Exposed type mapping:

| Exposed                | YDB                |
|------------------------|--------------------|
| `byte` / `ubyte`       | `Int8` / `Uint8`   |
| `short` / `ushort`     | `Int16` / `Uint16` |
| `integer` / `uinteger` | `Int32` / `Uint32` |
| `long`                 | `Int64`            |
| `float` / `double`     | `Float` / `Double` |
| `bool`                 | `Bool`             |
| `varchar` / `text`     | `Text`             |
| `binary` / `blob`      | `Bytes`            |
| `uuid`                 | `Uuid`             |
| `date`                 | `Date`             |
| `datetime`             | `Datetime`         |
| `timestamp`            | `Timestamp`        |
| `json`                 | `Json`             |
| `jsonb`                | `JsonDocument`     |

YDB-specific extensions are available through `ydb*` and `javatime.*`, for example:

```kotlin
ydbDecimal("price", precision = 10, scale = 2)
ydbInterval("duration")
ydbJson("payload")
ydbJsonDocument("indexed_payload")
ydbUuid("id")
ydbUint64("counter")
```

Signed temporal mode can be enabled through:

```kotlin
registerYdbDialect(enableSignedDatetimes = true)
```

When it is enabled, align the JDBC URL manually as well, for example:

```text
jdbc:ydb:grpc://localhost:2136/local?forceSignedDatetimes=true
```

If the URL already contains other query parameters, append the flag as
`&forceSignedDatetimes=true`.

## Schema management in production

Schema generation through Exposed is supported, but for YDB it is intentionally treated as a
compatibility workaround rather than the primary schema-management model.

In production, the recommended approach is:

1. manage schema through versioned migrations such as Flyway or Liquibase;
2. keep Exposed table definitions aligned with that schema;
3. validate drift through Exposed migration helpers.

If your application uses schema validation or migration diff generation through Exposed,
also add:

```xml
<dependency>
    <groupId>org.jetbrains.exposed</groupId>
    <artifactId>exposed-migration-core</artifactId>
    <version>${exposed.version}</version>
</dependency>
<dependency>
    <groupId>org.jetbrains.exposed</groupId>
    <artifactId>exposed-migration-jdbc</artifactId>
    <version>${exposed.version}</version>
</dependency>
```

This repository includes integration coverage for:

- manual schema creation through raw SQL;
- YDB-compatible drift detection for missing columns and secondary indexes;
- empty diff for matching schema;
- non-empty diff for drifted schema.

In Exposed 1.3.0, the full `MigrationUtils.statementsRequiredForDatabaseMigration(...)` path
unconditionally reads CHECK-constraint metadata from `INFORMATION_SCHEMA.CHECK_CONSTRAINTS`.
YDB does not expose that metadata through the current JDBC driver, so the repository validates
externally managed schemas through the compatible building blocks that Exposed already provides:

```kotlin
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import tech.ydb.exposed.dialect.ydbTransaction

ydbTransaction(db, readOnly = true) {
    val missingColumns = SchemaUtils.addMissingColumnsStatements(Products, withLogs = true)
    val existingIndexes = db.dialectMetadata.existingIndices(Products).getValue(Products)
}
```

When schema was changed through raw SQL just before validation, run the diff in a fresh transaction
so Exposed does not validate against stale metadata cache.

That validation path is the one that matters most in real projects, where schema changes are usually
applied by dedicated migration tools rather than by ORM-driven DDL generation.

## Known limitations

- Exposed 1.3.0 does not expose a dialect hook for PK rendering inside `CREATE TABLE`;
- every table intended for YDB DDL must override `createStatement()` and call `createYdbStatement()`;
- plain `Table` / `IdTable` DDL without that override emits inline PK SQL that YDB rejects;
- functional indexes are not supported;
- `ALTER TABLE ... ADD INDEX ... GLOBAL UNIQUE` depends on YDB support for unique indexes on existing tables;
- ANSI `MERGE` is not supported;
- `Uint64` binding is limited to the `0..Long.MAX_VALUE` range in the current implementation.

## Tests

Unit tests:

```bash
mvn test
```

Integration tests:

```bash
mvn verify
```

The build separates unit and integration tests through surefire/failsafe. Integration tests run
against YDB in testcontainers.

## Example module

The `example/` module demonstrates Exposed DSL usage together with the YDB-specific
`createYdbStatement()` override. Its connection bootstrap should follow the same
`registerYdbDialect(...) + Database.connect(...) + DatabaseConfig { ... }` recipe shown above.
