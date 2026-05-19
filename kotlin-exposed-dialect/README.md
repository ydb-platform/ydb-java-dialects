# Kotlin Exposed YDB Dialect

YDB integration for [JetBrains Exposed](https://github.com/JetBrains/Exposed) via JDBC.
The module provides:

- a Kotlin Exposed `VendorDialect` for YDB (SQL, type mapping, post-create indexes);
- [`YdbTable`](src/main/kotlin/tech/ydb/exposed/dialect/YdbTable.kt) for YQL `CREATE TABLE` (table-level PK, inline indexes, TTL);
- `Table.upsert` / `Table.replace` DSL backed by native YDB `UPSERT` / `REPLACE`;
- a retryable transaction wrapper that handles YDB's OCC retries transparently.

## Requirements

- JDK 17+
- Maven
- [YDB JDBC driver](https://github.com/ydb-platform/ydb-jdbc-driver) on the application classpath (not bundled with this artifact)
- JetBrains Exposed 1.x

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
import org.jetbrains.exposed.v1.jdbc.Database
import tech.ydb.exposed.dialect.registerYdbDialect
import tech.ydb.exposed.dialect.ydbTransaction

registerYdbDialect() // or registerYdbDialect(enableSignedDatetimes = true)

val db = Database.connect("jdbc:ydb:grpc://localhost:2136/local")

ydbTransaction(db) {
    // Exposed DSL / DAO code
}
```

## Defining tables

YDB requires a table-level `PRIMARY KEY (…)` in `CREATE TABLE`, not `col Type PRIMARY KEY` on a column.
Use [`YdbTable`](src/main/kotlin/tech/ydb/exposed/dialect/YdbTable.kt) for schema DDL; plain Exposed
`Table` + `SchemaUtils.create` still works for DML/tests but emits inline PK SQL that YDB rejects.

```kotlin
import tech.ydb.exposed.dialect.YdbIndexScope
import tech.ydb.exposed.dialect.YdbIndexSyncMode
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.javatime.ydbTimestamp64
import tech.ydb.exposed.dialect.ydbDecimal

object Products : YdbTable("products") {
    val id = integer("id")
    val sku = varchar("sku", 64)
    val name = varchar("name", 255)
    val category = varchar("category", 128)
    val price = ydbDecimal("price", precision = 10, scale = 2)
    val expiresAt = ydbTimestamp64("expires_at")

    override val primaryKey = PrimaryKey(id)

    init {
        // Post-create index (ALTER TABLE … ADD INDEX … GLOBAL) — same as on Exposed Table
        index(false, sku)

        // Inline index in CREATE TABLE (COVER / ASYNC / WITH)
        secondaryIndex(
            name = "products_category_idx",
            category,
            scope = YdbIndexScope.GLOBAL,
            syncMode = YdbIndexSyncMode.ASYNC,
            coverColumns = listOf(name, price)
        )

        ttl(expiresAt, "P30D")
    }
}
```

## Insert / upsert / replace / update / delete

Exposed's standard DSL works as-is. YDB's native `UPSERT` and `REPLACE` are exposed via
the same `Table.upsert` / `Table.replace` extensions Exposed provides for other vendors:

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

ANSI `MERGE` is intentionally rejected — `UPSERT` / `REPLACE` cover the same use cases.

YDB `UPSERT` writes only the columns listed in the DSL block; on primary-key conflict, other
columns are left unchanged. Exposed's `onUpdate` and `keyColumns` are **ignored** (no
`ON DUPLICATE KEY UPDATE`). `upsert(where)` **throws** — use `update { }` for conditional writes.

## Retryable transactions

YDB uses Optimistic Concurrency Control, so a transaction can fail with `Transaction locks
invalidated` under contention. Use `ydbTransaction` instead of plain `transaction` to retry
the body on retryable YDB statuses (`ABORTED`, `OVERLOADED`, `BAD_SESSION`, ...):

```kotlin
import tech.ydb.exposed.dialect.YdbRetryConfig
import tech.ydb.exposed.dialect.ydbTransaction

ydbTransaction(db) {
    // read-write; retries transient YDB statuses (ABORTED, OVERLOADED, BAD_SESSION, ...)
}

ydbTransaction(db, retry = YdbRetryConfig.IDEMPOTENT) {
    // idempotent body — UNDETERMINED and other non-transient retryable codes are retried too
}

ydbTransaction(db, readOnly = true, retry = YdbRetryConfig.IDEMPOTENT) {
    // read-only snapshot work
}
```

Backoff uses full jitter for `ABORTED` / `UNDETERMINED`, equal jitter for `UNAVAILABLE` / transport /
`OVERLOADED`, and zero delay for session errors. Status codes are read from `SQLException.errorCode`
(YDB vendor codes), not from error message text.

Use `retry = YdbRetryConfig.IDEMPOTENT` only when the body can be safely re-executed (pure reads,
single `UPSERT` / `REPLACE`, idempotent business logic). Customize attempts and backoff via
`YdbRetryConfig` or `YdbRetryConfig.DEFAULT.copy(maxAttempts = 3)`.

## Types

Default mapping for standard Exposed types:

| Exposed             | YDB                |
|---------------------|--------------------|
| `byte` / `ubyte`    | `Int8` / `Uint8`   |
| `short` / `ushort`  | `Int16` / `Uint16` |
| `integer`/`uinteger`| `Int32`/`Uint32`   |
| `long`              | `Int64`            |
| `float` / `double`  | `Float` / `Double` |
| `bool`              | `Bool`             |
| `varchar` / `text`  | `Text`             |
| `binary` / `blob`   | `Bytes`            |
| `uuid`              | `Uuid`             |
| `date`              | `Date`             |
| `datetime`          | `Datetime`         |
| `timestamp`         | `Timestamp`        |
| `json`              | `Json`             |
| `jsonb`             | `JsonDocument`     |

`varchar(n)` maps to `Text` (length is not preserved in YDB DDL).

### Production types (`ydb*` / `javatime.*`)

For temporal and unsigned columns, use **`ydbDate` / `ydbDate32`**, **`ydbUbyte`**, **`ydbUint32`**, etc.
They bind via YDB JDBC vendor type codes. Standard Exposed `date()`, `ubyte()`, `binary()` use
generic JDBC binding — DDL still maps correctly for many cases, but edge cases (unsigned ranges,
signed vs legacy temporal) may differ. **Prefer `ydb*` / `javatime.*` in production.**

Pick unsigned legacy or signed extended temporal types per column on any `Table`;
JDBC vendor code drives both bind and DDL `sqlType()`:

```kotlin
import tech.ydb.exposed.dialect.javatime.ydbDate
import tech.ydb.exposed.dialect.javatime.ydbDate32
import tech.ydb.exposed.dialect.javatime.ydbDatetime64

object Events : Table("events") {
    val created = ydbDate("created")           // Date
    val expires = ydbDate32("expires")         // Date32
    val updated = ydbDatetime64("updated")     // Datetime64
}
```

Optional: `registerYdbDialect(enableSignedDatetimes = true)` switches **dialect** DDL names for
standard Exposed `date` / `datetime` / `timestamp` to `Date32` / `Datetime64` / `Timestamp64`.
Add `forceSignedDatetimes=true` to the JDBC URL yourself when the driver requires it.
Per-column types remain explicit (`ydbDate` vs `ydbDate32`).

Additional YDB-specific column types are available via extension functions on `Table`:

```kotlin
ydbDecimal("price", precision = 10, scale = 2)
ydbInterval("duration")
ydbJson("payload")
ydbJsonDocument("indexed_payload")   // JsonDocument, analogue of jsonb
ydbUuid("id")                        // native Uuid; same as Exposed uuid() under this dialect
ydbUint64("counter")
```

`ydbUint64` / `ydbUlong` are backed by `Long` / `ULong` with range `0..Long.MAX_VALUE` for the
JDBC long path. Use a wider type if you need the full `Uint64` range.

For Decimal literals inside update expressions there's `ydbDecimalLiteral`:

```kotlin
import tech.ydb.exposed.dialect.ydbDecimalLiteral

it.update(Products.price, ydbDecimalLiteral(BigDecimal("45.00"), 10, 2))
```

## Identifiers

Exposed `autoIncrement()` maps to YDB `Serial` / `BigSerial`:

```kotlin
object Orders : YdbTable("orders") {
    val id = integer("id").autoIncrement()
    val total = ydbDecimal("total", precision = 12, scale = 2)
    override val primaryKey = PrimaryKey(id)
}
```

For UUID keys use `ydbUuid("id")` or Exposed `uuid()` under this dialect. Unsigned `Serial`
columns are not supported.

## Indexes

- **Post-create** (any `Table` or `YdbTable`): Exposed `index()` / `index(customName, isUnique, …)`
  → `ALTER TABLE … ADD INDEX … GLOBAL [UNIQUE] ON (…)`.
- **Inline in `CREATE TABLE`** (`YdbTable` only): [`secondaryIndex`](src/main/kotlin/tech/ydb/exposed/dialect/YdbTable.kt)
  with optional `COVER`, `ASYNC`, `WITH`.

## Known limitations

Inherited from Exposed `VendorDialect` unless overridden here: foreign keys, sequences,
`SELECT … FOR UPDATE`, dialect-specific features aimed at PostgreSQL/MySQL may produce SQL
that YDB does not support. This module overrides indexes, UPSERT/REPLACE, LIMIT/OFFSET, JSON
functions, and YDB type names — not the entire DDL surface.

- No ANSI `MERGE`; use `UPSERT` / `REPLACE`.
- Plain `Table` DDL uses Exposed's inline `PRIMARY KEY` on columns — use `YdbTable` (or hand-written YQL).
- No Yson / timezone-aware temporal types in this module.
- Functional indexes (Exposed `index` with expressions) are rejected.

## Tests

Integration tests use [testcontainers](https://www.testcontainers.org/) via
`tech.ydb.test:ydb-junit5-support` — no manual Docker setup needed:

```bash
mvn verify
```

DDL-focused tests use `YdbTable`; many other integration tests still use plain `Table` and may
fail `SchemaUtils.create` on YDB until migrated to `YdbTable` (inline `PRIMARY KEY` in Exposed DDL).

## Demo application

The `example/` module contains a runnable demo. Install the dialect first:

```bash
mvn -DskipTests -DskipITs install
```

Then run the demo:

```bash
cd example
mvn exec:java -Dexec.mainClass=tech.ydb.exposed.dialect.example.DemoAppKt
```

It expects a YDB instance at `jdbc:ydb:grpc://localhost:2136/local`.
