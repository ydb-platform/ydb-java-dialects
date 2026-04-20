## 0.1.0

Initial Kotlin Exposed dialect implementation for YDB.

### Added

- Added YDB dialect registration for Exposed JDBC.
- Added YDB SQL function provider.
- Added YDB-compatible `LIMIT` generation.
- Added YDB-compatible `UPSERT` generation for Exposed DSL.
- Added explicit rejection for ANSI `MERGE` with guidance to use `UPSERT`.
- Added YDB DDL generation for tables with mandatory primary keys.
- Added support for YDB secondary indexes / global secondary indexes.
- Added TTL helpers and TTL SQL generation.
- Added YDB data type provider.
- Added YDB-specific column type helpers for:
    - decimal values
    - interval values
    - JSON values
    - UUID values
    - unsigned integer values
- Added decimal literal helper for update-expression scenarios.
- Added UUID and ULID ID generation helpers.
- Added base table helpers for UUID/ULID identifiers.
- Added explicit rejection for `AUTO_INCREMENT`.
- Added retry classification for YDB retriable errors.
- Added transaction retry helper coverage.
- Added keyset pagination helper.
- Added optimistic locking helper based on a version column.
- Added metadata support for reading existing YDB indexes through JDBC metadata.
- Added console demo application with CRUD, UPSERT, indexes, decimal values, and keyset pagination.
- Added Docker-based integration test setup.
- Added GitHub Actions CI workflow.

### Tested

- Connection to local YDB.
- CRUD operations.
- UPSERT through Exposed DSL.
- Batch operations.
- DAO smoke workflow.
- Generated UUID/ULID identifiers.
- Secondary indexes.
- TTL.
- Numeric, binary, temporal, interval, decimal, UUID, unsigned integer, and JSON types.
- JOIN queries.
- Subqueries.
- Many-to-many relation through a join table.
- Optimistic locking.
- Keyset pagination.
- Integration scenario with several related tables.

### Limitations

- ANSI `MERGE` is not supported.
- SQL `AUTO_INCREMENT` is not supported.
- Unique secondary indexes are not treated as a supported portable feature.
- Foreign keys are not implemented as an enforced YDB constraint layer.
- Metadata support is focused on tested Exposed workflows rather than full schema introspection.
