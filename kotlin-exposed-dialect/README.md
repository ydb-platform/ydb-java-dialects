# Kotlin Exposed YDB Dialect

YDB integration for [JetBrains Exposed](https://github.com/JetBrains/Exposed) via JDBC.
The module provides:

- a Kotlin Exposed `VendorDialect` for YDB (DDL, SQL, type mapping, secondary indexes, TTL);
- `Table.upsert` / `Table.replace` DSL backed by native YDB `UPSERT` / `REPLACE`;
- a retryable transaction wrapper that handles YDB's OCC retries transparently.

## Requirements

- JDK 17+
- Maven
- YDB JDBC Driver
- JetBrains Exposed 1.x

## Quick start

```kotlin
import tech.ydb.exposed.dialect.connectYdb
import tech.ydb.exposed.dialect.ydbTransaction

val db = connectYdb(url = "jdbc:ydb:grpc://localhost:2136/local")

ydbTransaction(db) {
    // Exposed DSL / DAO code
}
```

[connectYdb](src/main/kotlin/tech/ydb/exposed/dialect/YdbDialectRegistration.kt) registers the YDB
JDBC driver and dialect metadata (idempotent), then opens an Exposed `Database` with defaults tuned
for YDB (`SERIALIZABLE` isolation, no nested transactions). Alternatively, call
`registerYdbDialect()` once and use `Database.connect("jdbc:ydb:...")`.

## Defining tables

YDB requires every table to declare a `PRIMARY KEY`. Inherit from `YdbTable` to get YDB-specific
DDL helpers on top of the standard Exposed `Table`:

```kotlin
import tech.ydb.exposed.dialect.YdbIndexScope
import tech.ydb.exposed.dialect.YdbIndexSyncMode
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.ydbDecimal

object Products : YdbTable("products") {
    val id = integer("id")
    val sku = varchar("sku", 64)
    val name = varchar("name", 255)
    val category = varchar("category", 128)
    val price = ydbDecimal("price", precision = 10, scale = 2)

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, sku)

        secondaryIndex(
            name = "products_category_idx",
            category,
            unique = false,
            scope = YdbIndexScope.GLOBAL,
            syncMode = YdbIndexSyncMode.ASYNC,
            coverColumns = listOf(name, price)
        )
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

Backoff and jitter follow the [.NET YDB SDK retry policy](https://github.com/ydb-platform/ydb-dotnet-sdk/tree/main/src/Ydb.Sdk/src/Ado/RetryPolicy):
full jitter for `ABORTED` / `UNDETERMINED`, equal jitter for `UNAVAILABLE` / transport /
`OVERLOADED`, zero delay for session errors. Status codes are read from `SQLException.errorCode`
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
| `binary` / `blob`   | `String`           |
| `uuid`              | `Uuid`             |
| `date`              | `Date`             |
| `datetime`          | `Datetime`         |
| `timestamp`         | `Timestamp`        |
| `json`              | `JsonDocument`     |

Pick unsigned legacy or signed extended temporal types per column on any `Table`
(including `YdbTable`); JDBC vendor code drives both bind and DDL `sqlType()`:

```kotlin
import tech.ydb.exposed.dialect.javatime.ydbDate
import tech.ydb.exposed.dialect.javatime.ydbDate32
import tech.ydb.exposed.dialect.javatime.ydbDatetime64

object Events : YdbTable("events") {
    val created = ydbDate("created")           // Date
    val expires = ydbDate32("expires")         // Date32
    val updated = ydbDatetime64("updated")     // Datetime64
}
```
`connectYdb` sets `forceSignedDatetimes=false` on the JDBC URL for driver compatibility;
per-column types are not controlled by a connection flag.

Additional YDB-specific column types are available via extension functions on `Table`:

```kotlin
ydbDecimal("price", precision = 10, scale = 2)
ydbInterval("duration")
ydbJson("payload")
ydbJsonDocument("indexed_payload")   // JsonDocument, analogue of jsonb
ydbUuid("id")                        // native Uuid; same as Exposed uuid() under this dialect
ydbUint64("counter")
```

`ydbUint64` is backed by `Long` and supports values `0..Long.MAX_VALUE`. Use a wider type
(`BigInteger`) if you need the full `Uint64` range.

For Decimal literals inside update expressions there's `ydbDecimalLiteral`:

```kotlin
import tech.ydb.exposed.dialect.ydbDecimalLiteral

it.update(Products.price, ydbDecimalLiteral(BigDecimal("45.00"), 10, 2))
```

## Identifiers

On `YdbTable`, Exposed `autoIncrement()` maps to YDB `Serial` / `BigSerial`:

```kotlin
object Orders : YdbTable("orders") {
    val id = integer("id").autoIncrement()
    val total = ydbDecimal("total", precision = 12, scale = 2)
    override val primaryKey = PrimaryKey(id)
}
```

For UUID keys use `ydbUuid("id")` or Exposed `uuid()` under this dialect. Unsigned `Serial`
columns are not supported.

## TTL

```kotlin
object Sessions : YdbTable("sessions") {
    val id = integer("id")
    val expireAt = timestamp("expire_at")
    override val primaryKey = PrimaryKey(id)

    init {
        ttl(expireAt, "PT1H")
    }
}
```

Numeric epoch columns are also supported via `YdbTtlColumnMode.SECONDS` /
`MILLISECONDS` / `MICROSECONDS` / `NANOSECONDS`.

## Tests

Integration tests use [testcontainers](https://www.testcontainers.org/) via
`tech.ydb.test:ydb-junit5-support` — no manual Docker setup needed:

```bash
mvn verify
```

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
