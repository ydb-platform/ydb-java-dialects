## 0.1.0

Initial release of the Kotlin Exposed dialect for YDB.

### Added

- YDB dialect registration for Exposed JDBC.
- Connection helper based on `YdbDialectProvider`.
- YDB-specific data type provider.
- YDB SQL function provider.
- `LIMIT` / `OFFSET` SQL generation.
- Native YDB `UPSERT` generation for Exposed DSL.
- Explicit handling of unsupported ANSI `MERGE` scenarios.
- YDB-compatible `CREATE TABLE` generation with mandatory primary key.
- Secondary index generation, including global indexes and cover columns.
- TTL support for supported YDB column modes.
- JDBC metadata support for reading existing indexes.
- Custom column types for decimal, interval, JSON, UUID variants and unsigned integer values.
- Decimal literal helper for update-expression scenarios.
- UUID and ULID generation helpers.
- YDB table helpers for UUID/ULID/string identifiers.
- Explicit rejection of SQL `AUTO_INCREMENT`.
- Retry classifier for common YDB retriable failures.
- Read-only and read-write retrying transaction helpers.
- Keyset pagination helpers.
- Optimistic locking helper based on a version column.
- Console demo application with CRUD, UPSERT, indexes and pagination.
- Docker Compose configuration for local YDB.
- Unit and integration test suites.
- GitHub Actions workflow for build and integration tests.

### Tested Scenarios

- Connection to local YDB through JDBC.
- Table creation and DDL generation.
- CRUD operations.
- UPSERT through Exposed DSL.
- Batch operations.
- DAO basic workflow.
- Generated UUID and ULID identifiers.
- Secondary indexes.
- TTL.
- Numeric, binary, temporal, interval, decimal, UUID, unsigned integer and JSON types.
- JOIN queries.
- Subqueries.
- Many-to-many relation through a join table.
- Optimistic locking.
- Keyset pagination.
- Multi-table integration scenario.
