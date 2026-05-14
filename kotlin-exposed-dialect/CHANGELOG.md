## 0.1.0

Initial release of the Kotlin Exposed dialect for YDB.

### Added

- YDB `VendorDialect` for Exposed JDBC, registered via `YdbDialectProvider.connect`.
- `ydbTransaction { ... }` / `ydbReadOnlyTransaction { ... }` — retryable transactions
  that classify failures via `YdbStatusable` (no fragile error-message parsing) and apply
  appropriate backoff for `ABORTED` / `OVERLOADED` / `BAD_SESSION` / `TRANSPORT_UNAVAILABLE`
  / `TIMEOUT` (idempotent only) / `UNDETERMINED` (idempotent only).
- Native `UPSERT` / `REPLACE` rendering — wired into Exposed's standard `Table.upsert` and
  `Table.replace` DSL.
- YDB-compatible `CREATE TABLE` generation with mandatory primary key.
- Secondary index DSL on `YdbTable` / `YdbIdTable` — global, async, cover columns, unique.
- TTL clause on `CREATE TABLE` / `ALTER TABLE`, plus numeric epoch modes.
- JDBC metadata for reading existing indexes.
- Default temporal mapping to extended types: `Date32` / `Datetime64` / `Timestamp64`.
  Legacy types are available via `YdbDialectProvider.connect(forceLegacyDatetimes = true)`.
- Custom column types for `Decimal`, `Interval`, `Json`, `JsonDocument`, three `Uuid`
  flavours and `Uint64`, plus a `ydbDecimalLiteral` helper for update expressions.
- Table base classes for generated identifiers: `YdbUuidIdTable`, `YdbUlidTable`,
  `YdbStringIdTable`.
- Explicit rejection of `AUTO_INCREMENT` and ANSI `MERGE` (`UPSERT` covers the use case).
- Console demo application showing CRUD, UPSERT and DDL.
- Integration tests powered by testcontainers via `tech.ydb.test:ydb-junit5-support`.
- GitHub Actions workflows for CI (`ci-exposed-ydb-dialect.yaml`) and Maven Central
  publishing (`publish-kotlin-exposed-dialect.yaml`).
