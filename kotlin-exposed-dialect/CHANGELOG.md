## 0.9.0

Initial release of the Kotlin Exposed dialect for YDB.

### Added

- YDB `VendorDialect` for Exposed JDBC and `registerYdbDialect(enableSignedDatetimes = ...)` for setup.
- Documented recommended YDB connection defaults for Exposed: `SERIALIZABLE`, `defaultReadOnly = false`, and `useNestedTransactions = false`.
- `ydbTransaction { ... }` with retry classification based on JDBC `SQLException` vendor codes.
- Native YDB `UPSERT` / `REPLACE` rendering wired into Exposed `Table.upsert` and `Table.replace`.
- `createYdbStatement()` for YDB-compatible `CREATE TABLE` rendering with a table-level `PRIMARY KEY (...)`.
- Post-create indexes through Exposed `Table.index()` -> `ALTER TABLE ... ADD INDEX ... GLOBAL`.
- JDBC metadata support for reading existing indexes from YDB.
- Temporal column extensions (`ydbDate`, `ydbDate32`, `ydbDatetime`, `ydbDatetime64`, `ydbTimestamp`, `ydbTimestamp64`) with JDBC vendor bindings.
- Custom YDB column types for `Decimal`, `Interval`, `Json`, `JsonDocument`, native `Uuid`, and unsigned values.
- `ydbDecimalLiteral` for decimal update expressions.
- `Serial` / `BigSerial` support via Exposed `autoIncrement()`.
- Explicit rejection of ANSI `MERGE`.

### Notes

- Exposed 1.3.0 does not provide a dialect hook for rendering a single-column PK inside `CREATE TABLE`, so schema generation for YDB is implemented through a `createStatement()` override and `createYdbStatement()`.
- Spring Boot integration is documented as an application-local configuration example on top of Exposed's official Spring support. The example mirrors the logic of the removed starter: explicit YDB driver selection, manual `forceSignedDatetimes=...` alignment in the JDBC URL, `Database` creation from Spring-managed `DataSource`, and direct use of `ydbTransaction(...)` for retry-aware paths.
- Production schema management is expected to use external versioned migrations; the repository includes validation coverage for externally created schemas through YDB-compatible Exposed drift checks for missing columns and secondary indexes.
