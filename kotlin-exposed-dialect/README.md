# Kotlin Exposed YDB Dialect

YDB integration for [JetBrains Exposed](https://github.com/JetBrains/Exposed) via JDBC.
The module provides:

- a Kotlin Exposed `VendorDialect` for YDB (DDL, SQL, type mapping, secondary indexes, TTL);
- `Table.upsert` / `Table.replace` DSL backed by native YDB `UPSERT` / `REPLACE`;
- a retryable transaction wrapper that handles YDB's OCC retries transparently;
- table base classes for tables with generated identifiers (UUID, ULID, ...).

## Requirements

- JDK 17+
- Maven
- YDB JDBC Driver
- JetBrains Exposed 1.x

## Quick start

```kotlin
import tech.ydb.exposed.dialect.YdbDialectProvider
import tech.ydb.exposed.dialect.ydbTransaction

val db = YdbDialectProvider.connect(
    url = "jdbc:ydb:grpc://localhost:2136/local"
)

ydbTransaction(db) {
    // Exposed DSL / DAO code
}
```

`YdbDialectProvider.connect` registers the YDB JDBC driver and dialect metadata exactly once,
then opens an Exposed `Database` with a sane default configuration
(`SERIALIZABLE` isolation, no nested transactions).

## Defining tables

YDB requires every table to declare a `PRIMARY KEY`. Inherit from `YdbTable` to get YDB-specific
DDL helpers on top of the standard Exposed `Table`:

```kotlin
import tech.ydb.exposed.dialect.YdbIndexScope
import tech.ydb.exposed.dialect.YdbIndexSyncMode
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.types.ydbDecimal

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

For tables that need to participate in Exposed DAO, use `YdbIdTable` (or its specializations
`YdbUuidIdTable`, `YdbUlidTable`, `YdbStringIdTable`).

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
import tech.ydb.exposed.dialect.ydbTransaction
import tech.ydb.exposed.dialect.ydbReadOnlyTransaction

ydbTransaction(db) {
    // read-write, non-idempotent
}

ydbTransaction(db, idempotent = true) {
    // single UPSERT / pure read body — TIMEOUT / UNDETERMINED also retried
}

ydbReadOnlyTransaction(db) {
    // shortcut for idempotent read-only work
}
```

Set `idempotent = true` only when the body can be safely re-executed (pure reads, single
`UPSERT` / `REPLACE`, idempotent business operation). The classifier inspects YDB status codes
via `YdbStatusable` rather than parsing error message text.

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
| `date`              | `Date32`           |
| `datetime`          | `Datetime64`       |
| `timestamp`         | `Timestamp64`      |
| `json`              | `JsonDocument`     |

Temporal columns default to YDB **extended** types (`Date32`, `Datetime64`, `Timestamp64`).
To target the legacy unsigned types when integrating with an existing schema, pass
`forceLegacyDatetimes = true`:

```kotlin
val db = YdbDialectProvider.connect(
    url = "jdbc:ydb:grpc://localhost:2136/local",
    forceLegacyDatetimes = true  // emits Date / Datetime / Timestamp
)
```

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
import tech.ydb.exposed.dialect.types.ydbDecimalLiteral

it.update(Products.price, ydbDecimalLiteral(BigDecimal("45.00"), 10, 2))
```

## Identifiers

YDB does not expose `AUTO_INCREMENT`. The dialect explicitly rejects `autoIncrement()`.
Use one of the IdTable base classes instead:

- `YdbUuidIdTable` — native YDB `Uuid` column, auto-generated via `UUID.randomUUID()`;
- `YdbUlidTable` — 26-char [ULID](https://github.com/ulid/spec), lexicographically sortable;
- `YdbStringIdTable` — caller-provided business key.

A top-level `ydbUlid()` is also exposed for generating ULIDs manually.

```kotlin
import tech.ydb.exposed.dialect.YdbUlidTable

object Events : YdbUlidTable("events") {
    val payload = text("payload")
}
```

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
