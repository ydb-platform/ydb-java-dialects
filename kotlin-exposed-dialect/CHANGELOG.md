## 0.9.0

Initial release of the Kotlin Exposed dialect for YDB.

### Added

- YDB `VendorDialect` for Exposed JDBC; `registerYdbDialect(enableSignedDatetimes = …)` for setup.
- `ydbTransaction { ... }` — retryable transactions with `readOnly` and `YdbRetryConfig`
  (exponential backoff with full/equal jitter; retries classified by JDBC `SQLException` vendor codes).
- Native `UPSERT` / `REPLACE` rendering — wired into Exposed's standard `Table.upsert` and
  `Table.replace` DSL.
- `YdbTable` — YQL `CREATE TABLE` with table-level `PRIMARY KEY (…)`, inline secondary indexes
  (`secondaryIndex`), and optional row TTL (`ttl` / `YdbTtlColumnMode`); prefer over plain Exposed
  `Table` when DDL must be valid on YDB.
- Post-create indexes via Exposed `Table.index()` on any table → `ALTER TABLE … ADD INDEX … GLOBAL`.
- JDBC metadata for reading existing indexes.
- Temporal columns: unsigned (`ydbDate`, …) and signed (`ydbDate32`, …) extensions with
  JDBC vendor codes; DDL `sqlType()` derived from the code. No connection-level temporal flag.
- Custom column types for `Decimal`, `Interval`, `Json`, `JsonDocument`, native `Uuid`,
  unsigned integers, `Uint64`, plus a `ydbDecimalLiteral` helper for update expressions.
- `Serial` / `BigSerial` via Exposed `autoIncrement()` on `Table`.
- Explicit rejection of ANSI `MERGE` (`UPSERT` covers the use case).
- Console demo application showing CRUD, UPSERT and DDL.
- Integration tests powered by testcontainers via `tech.ydb.test:ydb-junit5-support`.
- Optional `kotlin-exposed-ydb-dialect-spring-boot-starter` module that layers YDB dialect
  registration, datasource URL normalization, and retry-aware transactions on top of
  Exposed's official Spring Boot starter.
- GitHub Actions workflows for CI (`ci-exposed-ydb-dialect.yaml`) and Maven Central
  publishing (`publish-kotlin-exposed-dialect.yaml`).
