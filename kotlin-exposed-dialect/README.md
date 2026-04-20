## Kotlin Exposed YDB Dialect

SQL dialect and JDBC integration layer that allows using [JetBrains Exposed](https://github.com/JetBrains/Exposed) DSL/DAO with [YDB](https://ydb.tech).

The module provides YDB-specific SQL generation, DDL support, type mappings, UPSERT support, transaction helpers, pagination helpers, tests, CI, and a small demo application.

### Module Coordinates

```xml
<dependency>
    <groupId>tech.ydb.dialects</groupId>
    <artifactId>kotlin-exposed-ydb-dialect</artifactId>
    <version>0.1.0</version>
</dependency>
```

The artifact is currently intended to be built from this repository:

```bash
mvn clean install
```

### Requirements

- JDK 17+
- Maven
- Docker / Docker Compose for integration tests and local demo
- Local YDB instance for integration tests

The module uses the YDB JDBC driver and Exposed 1.x APIs.

### Quick Start

Start local YDB:

```bash
docker compose up -d
```

Connect to YDB through the dialect provider:

```kotlin
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import tech.ydb.exposed.dialect.YdbDialectProvider

val database = YdbDialectProvider.connect(
    url = "jdbc:ydb:grpc://localhost:2136/local",
    user = "",
    password = ""
)

transaction(database) {
    // Exposed DSL / DAO code
}
```

### Table Example

YDB requires every table to have an explicit primary key.

```kotlin
import org.jetbrains.exposed.v1.core.Table
import tech.ydb.exposed.dialect.ddl.secondaryIndex
import tech.ydb.exposed.dialect.types.ydbDecimal

object Products : Table("products") {
    val id = integer("id")
    val sku = varchar("sku", 64)
    val name = varchar("name", 255)
    val category = varchar("category", 128)
    val price = ydbDecimal("price", precision = 22, scale = 9)

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, sku)
        secondaryIndex("products_category_idx", category)
    }
}
```

### UPSERT

YDB has native `UPSERT`, and the dialect maps Exposed `upsert` calls to YDB-compatible SQL.

```kotlin
import org.jetbrains.exposed.v1.jdbc.upsert
import tech.ydb.exposed.dialect.types.ydbDecimalLiteral
import java.math.BigDecimal

Products.upsert {
    it[id] = 1
    it[sku] = "book-001"
    it[name] = "YDB recipes"
    it[category] = "books"
    it[price] = ydbDecimalLiteral(BigDecimal("19.990000000"))
}
```

YDB requires an explicit column list for `UPSERT INTO ... VALUES (...)`; the dialect generates it automatically for Exposed DSL calls.

### Generated IDs

YDB does not support SQL `AUTO_INCREMENT` in the usual relational database sense. The dialect rejects `autoIncrement()` explicitly.

For application-side generated identifiers, use the provided helpers:

```kotlin
import tech.ydb.exposed.dialect.basic.YdbUuidStringIdTable
import tech.ydb.exposed.dialect.basic.YdbUlidTable

object Users : YdbUuidStringIdTable("users") {
    val name = varchar("name", 255)
}

object Events : YdbUlidTable("events") {
    val payload = text("payload")
}
```

Available helpers include:

- `YdbUuidIdTable`
- `YdbUuidStringIdTable`
- `YdbUlidTable`
- `YdbGeneratedIds.uuid()`
- `YdbGeneratedIds.uuidString()`
- `YdbGeneratedIds.ulid()`

### Type Mapping

The dialect provides YDB-aware mappings for common Exposed column types and additional YDB-specific helpers.

Supported groups:

- integer types: `Int16`, `Int32`, `Int64`
- unsigned integer helpers, including `Uint64`
- floating-point types: `Float`, `Double`
- `Bool`
- text/string values
- binary values
- `Date`
- `Datetime`
- `Timestamp`
- `Interval`
- `Decimal(p, s)`
- UUID:
    - native YDB UUID representation
    - UUID as UTF-8 string
    - UUID as bytes
- JSON

For decimal columns and decimal update expressions, prefer the dialect helpers:

```kotlin
val price = ydbDecimal("price", precision = 22, scale = 9)
```

```kotlin
it[price] = ydbDecimalLiteral(BigDecimal("12.490000000"))
```

### DDL Support

Supported DDL features:

- `CREATE TABLE`
- mandatory `PRIMARY KEY`
- YDB-compatible column type generation
- secondary indexes / global secondary indexes
- TTL helpers
- `ALTER TABLE ... ADD INDEX` for supported secondary index scenarios

### TTL

The dialect includes helpers for YDB TTL expressions and table-level TTL settings. TTL support is covered by unit and integration tests for several supported YDB column modes.

### Pagination

Standard Exposed `LIMIT` is supported.

The module also provides keyset pagination helpers for YDB-friendly pagination over ordered keys:

```kotlin
import tech.ydb.exposed.dialect.pagination.keysetPageAsc

val page = Products
    .selectAll()
    .orderBy(Products.id)
    .keysetPageAsc(Products.id, lastValue = null, limit = 20)
    .toList()
```

### Transactions and Retries

The dialect works with standard Exposed JDBC transactions on top of the YDB JDBC driver.

The project also includes retry classification and retry helpers for typical YDB retriable failures, including abort and timeout-like scenarios. Retry behavior is covered by unit tests and integration smoke tests.

### DSL and DAO Compatibility

The following Exposed scenarios are covered by tests:

- basic connection
- CRUD
- UPSERT
- batch operations
- DAO smoke workflow
- generated UUID/ULID identifiers
- joins
- subqueries
- many-to-many relation through a join table
- optimistic locking with a version column
- keyset pagination
- secondary indexes
- TTL
- YDB-specific types

### MERGE Support

ANSI `MERGE` is not implemented by this dialect.

YDB has native `UPSERT`, but Exposed `MERGE` has broader conditional semantics. Translating Exposed `MERGE` to YDB `UPSERT` would be misleading, so the dialect explicitly rejects `MERGE` calls with an `UnsupportedOperationException`.

Use `upsert` or batch UPSERT-style operations instead.

### Limitations

Important limitations:

- Every YDB table must have an explicit primary key.
- `AUTO_INCREMENT` is not supported. Use application-side UUID/ULID generation.
- ANSI `MERGE` is not supported. Use `UPSERT`.
- Unique secondary indexes are not treated as a supported portable feature in this dialect.
- Foreign keys are not enforced as a primary modeling mechanism for YDB in this project.
- Schema metadata support is intentionally focused on the parts Exposed needs for tested workflows, especially table/index inspection.
- Some advanced YDB/YQL features are outside the current Exposed dialect surface.

### Running Tests

Start local YDB:

```bash
docker compose up -d
```

Run all unit and integration tests:

```bash
mvn clean install
```

The build runs:

- unit tests through Surefire
- integration tests through Failsafe
- packaging and local Maven installation

Stop local YDB:

```bash
docker compose down -v
```

### Demo Application

The demo application shows the dialect against a real local YDB instance.

It demonstrates:

- YDB connection through `YdbDialectProvider`
- table creation
- primary key requirement
- secondary index creation
- UPSERT seed data
- read queries
- update
- delete
- decimal values
- keyset pagination
- generated DDL output

Start YDB:

```bash
docker compose up -d
```

Run demo on Linux, macOS, or cmd:

```bash
mvn compile exec:java -Dexec.mainClass="tech.ydb.exposed.dialect.demo.DemoAppKt"
```

Run demo in PowerShell:

```powershell
mvn --% compile exec:java -Dexec.mainClass=tech.ydb.exposed.dialect.demo.DemoAppKt
```

By default, the demo uses:

```text
jdbc:ydb:grpc://localhost:2136/local
```

You can override connection settings with environment variables:

- `YDB_JDBC_URL`
- `YDB_USER`
- `YDB_PASSWORD`

### CI

The repository contains a GitHub Actions workflow for this module.

The CI workflow:

- checks out the repository
- sets up JDK
- starts local YDB with Docker Compose
- waits until YDB is reachable
- runs `mvn clean install`
- prints YDB logs on failure
- stops and removes the local YDB container

### Project Structure

Main areas:

- `src/main/kotlin/tech/ydb/exposed/dialect/basic`
  Dialect registration, metadata, generated ID helpers, table helpers.

- `src/main/kotlin/tech/ydb/exposed/dialect/functions`
  SQL function provider, LIMIT, UPSERT, MERGE rejection.

- `src/main/kotlin/tech/ydb/exposed/dialect/types`
  YDB-specific column types and type helpers.

- `src/main/kotlin/tech/ydb/exposed/dialect/ddl`
  YDB DDL extensions such as secondary indexes and TTL.

- `src/main/kotlin/tech/ydb/exposed/dialect/pagination`
  Keyset pagination helpers.

- `src/main/kotlin/tech/ydb/exposed/dialect/transaction`
  Retry classification and transaction helpers.

- `src/main/kotlin/tech/ydb/exposed/dialect/demo`
  Console demo application.

- `src/test/kotlin`
  Unit and integration tests.

### Current Status

The dialect is implemented as a working YDB integration for Exposed DSL/DAO within the tested feature set.

Current test coverage includes:

- 61 unit tests
- 48 integration tests

The latest local verification passed with:

```text
Tests run: 61, Failures: 0, Errors: 0, Skipped: 0
Tests run: 48, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
