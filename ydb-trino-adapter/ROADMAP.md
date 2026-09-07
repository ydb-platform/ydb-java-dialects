# YDB Trino Adapter — roadmap

This roadmap tracks the connector against Trino 483
`BaseConnectorTest`/`BaseConnectorSmokeTest`. A green test is meaningful only
when it exercises the advertised behavior. Empty overrides and false capability
flags are test debt, not support.

## Trino 483 dependency migration

The module targets [Trino 483](https://github.com/trinodb/trino/releases/tag/483),
published on [Maven Central](https://repo.maven.apache.org/maven2/io/trino/trino-spi/483/).
It requires 64-bit Java 25, with a minimum patch level of 25.0.1; the module and
CI continue to use Java 25.

The 479-to-483 source migration consists of the following upstream contract
changes:

- `ColumnMetadata.getComment()` now returns `Optional<String>`;
- JDBC merge rollback registration now accepts `Consumer<Runnable>`;
- JDBC create-table rollback registration uses the same callback shape, and
  destination-table cleanup is now separate from temporary-table cleanup; the
  adapter inherits both implementations;
- page-sink provider methods now receive optional table credentials, which are
  intentionally unused because YDB writes remain authenticated through their
  JDBC connection;
- `MODULO_FUNCTION_NAME` replaces the deprecated
  `MODULUS_FUNCTION_NAME` connector-expression constant.

The SPI also removed the deprecated session-taking `Type.getObject` overload
and `Type.appendTo`; the adapter already uses the retained
`Type.getObject(Block, int)` method.

`QueryBuilder` is source-identical between 479 and 483, and the relevant
`JdbcPageSink`/`JdbcMergeSink` constructors are unchanged. The
`TestingConnectorBehavior` enum has no additions, removals, renamed values, or
default changes. The active inherited-test delta adds
`testVarcharEqualityPushdownIgnoresTrailingSpaces`; it must pass against YDB
before this upgrade can be declared green. The new materialized-view `WHEN
STALE` tests remain skipped by the existing unsupported materialized-view
capability. Trino 483 removes the inherited `testDropTableIfExists`,
`testMaterializedViewWhenStale`, `testShowCreateInformationSchema`,
`testShowCreateInformationSchemaTable`, `testShowInformationSchemaTables`, and
`testSymbolAliasing` methods, plus the duplicate smoke-test information-schema
method.

The upgrade was merged as PR #252 (`ddd8133`). GitHub Actions on that PR ran
the complete Docker-backed suite: 305 tests, 0 failures, 0 errors, 85 skipped
(220 passed), `BUILD SUCCESS`. Every Trino release-coupled artifact resolves to
483. That run is the post-upgrade CI baseline for later scoped PRs.

## Pre-upgrade DML baseline (Trino 479)

PR #240 adds DELETE, UPDATE, row-level UPDATE, and MERGE. The following pieces
have been verified locally against a real YDB test container:

- typed predicates for DELETE and UPDATE, including `varchar` values;
- exact affected-row counts through YQL `RETURNING`, without a racy pre-count;
- `Float`/`Double` write mappings during MERGE;
- real JDBC primary-key metadata, including `KEY_SEQ` ordering;
- concurrent updates identified by the test-only hidden primary key;
- smoke MERGE and row-level UPDATE without disabling their behavior flags;
- retry classification through the YDB SDK status model, with fresh merge
  connections and rollback-before-close.

GitHub Actions on PR #240 verified the DML baseline: 314 tests run, 0 failures,
0 errors, and 84 skipped. This included 36 smoke tests (4 skipped) and 278
connector tests (80 skipped). The inherited `testMergeLarge` runs without an
override and completes within the CI budget. An isolated local Colima run
previously exceeded 11 minutes, so MERGE scalability remains a production
concern rather than a CI failure.

## Approved namespace and catalog contract

The approved public namespace model is:

- one Trino catalog is one configured YDB connector instance bound
  to exactly one YDB database through its JDBC `connection-url` and credentials;
- multiple YDB databases use multiple Trino catalog configurations; the
  connector does not discover databases or publish catalogs dynamically;
- the connector exposes exactly one virtual, non-droppable Trino schema named
  `default`;
- a Trino table name is the complete YDB object path relative to the configured
  database root. Root table `orders` is `catalog.default.orders`; nested table
  `sales/eu/orders` is `catalog.default."sales/eu/orders"`;
- the configured database path is a connection and security boundary and never
  becomes part of the Trino schema or table name.

This keeps the native YDB path visible in SQL and avoids flattening directories
into a list of artificial Trino schemas. Trino schemas are not hierarchical, so
mapping `sales/eu` to a schema would still appear as one flat, quoted schema in
SQL clients rather than as a directory tree.

### Metadata and DDL behavior

| Trino operation | Contract |
| --- | --- |
| `listSchemaNames()` | Return only `default` |
| `listTables(Optional.empty())` | Return visible YDB tables recursively, all in `default` |
| `listTables(Optional.of("default"))` | Same table set as the unfiltered call |
| `listTables()` for another schema | Return an empty list |
| `getTableHandle(default, name)` | Resolve the validated full relative YDB path |
| `getTableHandle()` for another schema | Return empty without querying an unrelated path |
| `CREATE`, `DROP`, `RENAME SCHEMA` | `NOT_SUPPORTED`; `default` is virtual and cannot be changed |
| `CREATE TABLE default."dir/table"` | Require the parent YDB directory to exist |
| table rename | May move a table by changing its full relative path; target parent must exist |

The connector accepts relative paths without empty, `.` or `..` components
and quotes the full path as one YQL identifier. YDB validates component names,
lengths and depth. Dot-prefixed tables reported by JDBC remain visible.

Trino 483 lowercases identifiers; YDB paths are case-sensitive. The connector
resolves unique case-insensitive matches and preserves remote spelling for
reads, writes and DDL. Ambiguous matches fail explicitly. CREATE/CTAS/RENAME
also resolve existing parent directories, including empty directories, through
the JDBC-owned SchemeClient. Directories remain parts of table names.

Invalid relative paths resolve to no table during lookup and fail with
`INVALID_ARGUMENTS` during CREATE/CTAS/RENAME. Other lookup names, including
Trino's `"<table>$data"` probe, are matched against JDBC metadata normally.
JDBC errors propagate instead of being treated as missing tables.

`usePrefixPath` is rejected because it changes the configured database root.
Trino 483 suppresses remote metadata errors for exact-name
`information_schema.columns` lookups; broad bulk listing and direct access
still report case collisions. Metadata resolution does not lock concurrent
external namespace changes.

### Implemented namespace slice

This slice implements the following items:

1. Added relative-path checks for lookup and table operations.
2. Changed the synthetic schema from `ydb` to `default`.
3. Made `listTables` and `getTableHandle` honor the schema filter and preserve
   the complete relative YDB table path.
4. Mapped only genuine remote not-found results and structurally invalid
   lookup names to an absent table handle and preserved other JDBC/YDB
   failures.
5. Added focused tests for root and nested tables, an unknown schema, path
   traversal, dot-prefixed paths, the `$data` probe, case-only ambiguity,
   mixed-case write/rename lifecycle, and full-path quoting.
6. Made relation-comment metadata and opt-in bulk column metadata honor the
   virtual `default` schema, recursive paths, and case-only collision checks.
   Comment writes remain unsupported, and bulk column metadata remains opt-in.

This slice does not enable schema DDL capabilities or change capability flags.
Catalog provisioning and cross-catalog federation remain a separate, unverified
integration slice.

## P0 — production CREATE/CTAS primary-key definition

Production `YdbClient.createTableSqls` still delegates to generic JDBC SQL,
which does not declare a YDB primary key. The inherited test fixture injects a
hidden key through `TestingYdbJdbcClient`. Consequently, its green CREATE/CTAS
cases verify namespace and framework behavior with that fixture, not a complete
production table-definition contract. Define an explicit production key model
and verify it through the production client in a separate feature before
claiming production CREATE/CTAS support. YDB requires a non-empty
[primary key](https://ydb.tech/docs/en/yql/reference/syntax/create_table).

## P0 — harden MERGE scalability and atomicity

1. Replace row-by-row prepared-statement execution in `YdbMergeSink` with a
   set-based YQL path. Candidate primitives are
   [`UPDATE ... ON`](https://ydb.tech/docs/en/yql/reference/syntax/update) and
   [`AS_TABLE`](https://ydb.tech/docs/en/yql/reference/syntax/select/from_as_table),
   using bounded batches of typed list-of-struct parameters.
2. Preserve one logical MERGE transaction. Until a staging/finalize design is
   implemented, keep one writer task and reject Trino query/task retries for a
   direct-to-target merge.
3. YQL `UPDATE` cannot change a primary-key value. Implement physical-key
   changes as atomic delete+insert row changes, or reject that statement with a
   documented `NOT_SUPPORTED` error. See the
   [YQL UPDATE contract](https://ydb.tech/docs/en/yql/reference/syntax/update).
4. Add focused tests for composite primary keys, a non-unique first visible
   column, physical-key updates, rollback/close, and fresh-state retries.

**Exit criterion:** retain the current green inherited `testMerge*` suite and
add a bounded-memory benchmark that demonstrates acceptable production-scale
runtime for the set-based implementation.

## P1 — retry and transaction hardening

- Retry only statuses classified as unconditional by the pinned YDB SDK.
  `TIMEOUT`, `UNDETERMINED`, transport failures, and other conditional statuses
  are unsafe for non-idempotent writes unless an operation-id/staging design
  proves replay safety. See
  [YDB SDK error handling](https://ydb.tech/docs/en/reference/ydb-sdk/error_handling).
- Keep the JDBC `SessionPool.acquire` scheduler-rejection workaround limited to
  that provably pre-execution stack. Track it against the YDB JDBC driver and
  remove the connector workaround after upgrading to a fixed driver.
- YDB JDBC 2.3.18 connection-context caching has a close/register race under
  concurrent connections. The connector therefore defaults
  `cacheConnectionsInDriver` to `false`; an explicit JDBC URL option can
  override it. This trades connection reuse for correctness and may increase
  connection latency/load. Re-evaluate the default after upgrading to a driver
  with a verified cache-lifecycle fix.
- Do not replay buffered INSERT pages after `JdbcPageSink` may already have
  committed an internal batch.
- Add unit tests for status classification, interrupted backoff, rollback
  failure suppression, connection cleanup, and a failure after commit.
- Define memory/backpressure limits for buffered merge pages; memory usage must
  not remain unreported.

Concurrent `ALTER TABLE ... ADD COLUMN` statements on one table can be rejected
by YDB with `OVERLOADED` (400060) and the specific issue `path is under
operation` in `EPathStateAlter`. The inherited Trino test permits only this
exact connector-specific conflict and still verifies every successfully added
column. This contract does not add an `ADD COLUMN` retry. The connector uses
Trino's one-shot `BaseJdbcClient` statement execution for DDL, so a transient
failure cannot replay a non-idempotent statement on the same connection. See
the [YDB status-code contract](https://ydb.tech/docs/en/reference/ydb-sdk/ydb-status-codes).

## P2 — remove remaining test debt

Audit every inherited-test override and every `hasBehavior` exception. For each
unsupported behavior, record:

1. the exact YDB/YQL or Trino limitation;
2. an authoritative documentation link or tracked upstream issue;
3. a focused negative test that proves the connector fails clearly.

The existing negative-date and cast-pushdown overrides need this treatment.
YDB `Date` starts at the Unix epoch; see
[primitive types](https://ydb.tech/docs/en/yql/reference/types/primitive).
CHAR is rejected with the focused inherited contract
(`Unsupported column type: char(3)`) because YDB has no fixed-width string
primitive and mapping it to `Text` would lose Trino padding semantics.

Nullable `ADD COLUMN` remains supported. The separate
`SUPPORTS_ADD_COLUMN_NOT_NULL_CONSTRAINT` capability remains false because the
connector explicitly rejects `ADD COLUMN ... NOT NULL`; Trino's inherited
`testAddNotNullColumnToEmptyTable` verifies that rejection. Current YQL syntax
documents `NOT NULL`, but the capability must not be enabled until the YDB
image used by the test suite passes Trino's complete empty-table,
non-empty-table, and nullability-metadata contract. Column defaults remain a
separate capability. See YDB
[`ALTER TABLE ... ADD COLUMN`](https://ydb.tech/docs/en/yql/reference/syntax/alter_table/columns).

The former empty default-column INSERT override has been replaced with a
YDB-native row-table fixture. The inherited `testInsertForDefaultColumn` now
verifies omitted literal defaults, explicit values and nulls, and reordered
insert columns. YDB supports literal defaults on row-oriented tables, but the
Trino 483 `SUPPORTS_DEFAULT_COLUMN_VALUE` behavior remains false because its
group also advertises CREATE, ADD, NOT NULL/default, and MERGE contracts that
the connector does not fully implement. SET and DROP have child capabilities
that inherit from this behavior and also remain false. See YDB
[`CREATE TABLE`](https://ydb.tech/docs/en/yql/reference/syntax/create_table).

## Later capability work

- List/Dict/Struct mappings for Trino ARRAY/MAP/ROW;
- views, comments, rename column, and type changes after checking current YQL
  semantics;
- transactional INSERT/staging instead of direct non-transactional writes;
- complete the Trino 483 default-column behavior group before advertising it.

## Validation ladder

Use JDK 25 at patch level 25.0.1 or newer and the Docker/Testcontainers
environment documented in the root `AGENTS.md`:

```bash
mvn -f ydb-trino-adapter/pom.xml -DskipTests compile
mvn -f ydb-trino-adapter/pom.xml -Dtest='TestYdbConnectorTest#testName' test
mvn -f ydb-trino-adapter/pom.xml -Dtest=TestYdbConnectorSmokeTest test
mvn --batch-mode --update-snapshots -f ydb-trino-adapter/pom.xml clean test
```

Always report the test count and skipped count. Do not disable Docker-backed
tests, weaken CI, or add a no-op override to obtain a green result.
