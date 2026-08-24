# YDB Trino Adapter — roadmap

This roadmap tracks the connector against Trino 479
`BaseConnectorTest`/`BaseConnectorSmokeTest`. A green test is meaningful only
when it exercises the advertised behavior. Empty overrides and false capability
flags are test debt, not support.

## Current DML status

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

GitHub Actions run
[`32377210642`](https://github.com/ydb-platform/ydb-java-dialects/actions/runs/32377210642)
on 2026-08-20 at `619b33d` reported 336 tests: 0 failures, 0 errors, and
84 skipped, leaving 252 executed tests passed. It included Federation 6/0/0/0,
the non-ephemeral port allocator 3/0/0/0, Connector 286/80, Smoke 36/4, and
TablePath 5/0.

## Extended temporal contract

The pinned YDB JDBC driver is 2.3.18. Its extended temporal mode is controlled
by `forceSignedDatetimes`, declared by
[`YdbOperationProperties.FORCE_NEW_DATETYPES`](https://github.com/ydb-platform/ydb-jdbc-driver/blob/v2.3.18/jdbc/src/main/java/tech/ydb/jdbc/settings/YdbOperationProperties.java#L60-L62),
and defaults to `false` in the driver. The connector now supplies
`forceSignedDatetimes=true` by default. An explicit value in the JDBC URL still
wins over supplied connection properties and must not be set to `false` for
this connector contract.

- new Trino `date` writes and DDL use YDB `Date32`;
- new Trino `timestamp(6)` writes and DDL use YDB `Timestamp64`;
- existing `Date`/`Date32`, `Datetime`/`Datetime64`, and
  `Timestamp`/`Timestamp64` columns map respectively to Trino `date`,
  `timestamp(0)`, and `timestamp(6)`;
- `Timestamp64` is read and written through JDBC `Instant` at UTC, avoiding the
  driver's JVM-default-zone `LocalDateTime` conversion and preserving
  microseconds;
- predicate pushdown is limited to the range of the physical YDB temporal type,
  so an out-of-range Trino constant is evaluated by Trino instead of being
  bound to JDBC.

The type name is `Timestamp64`, not `Timestampt64`. The contract and ranges
come from the [YDB primitive type reference](https://ydb.tech/docs/en/yql/reference/types/primitive).
The driver flag and signed mappings were introduced in
[`ydb-jdbc-driver` PR #116](https://github.com/ydb-platform/ydb-jdbc-driver/pull/116).
Focused unit coverage has 7 tests, and two real-YDB integration tests cover
pre-1970 values, nulls, second-versus-microsecond precision, legacy and wide
types in the same table, predicate binding, and the physical types created by
Trino.

## Approved namespace and catalog contract

The public namespace model was agreed on 2026-08-19:

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
| `listTables(Optional.empty())` | Return every accessible YDB table recursively, all in `default` |
| `listTables(Optional.of("default"))` | Same table set as the unfiltered call |
| `listTables()` for another schema | Return an empty list |
| `getTableHandle(default, name)` | Resolve the validated full relative YDB path |
| `getTableHandle()` for another schema | Return empty without querying an unrelated path |
| `CREATE`, `DROP`, `RENAME SCHEMA` | `NOT_SUPPORTED`; `default` is virtual and cannot be changed |
| `CREATE TABLE default."dir/table"` | Require the parent YDB directory to exist |
| table rename | May move a table by changing its full relative path; target parent must exist |

The connector must parse paths into components before producing YQL. It rejects
absolute paths, empty components, leading/trailing or repeated `/`, `.` and
`..`, dot-prefixed/system components, invalid YDB component names, and
components longer than the 255-character YDB component limit. It also rejects
paths deeper than YDB's 32-component database limit. It does not impose an
artificial 255-character limit on the complete relative path. The validated full
path is quoted as one YQL identifier through the connector quoting helper. It is
not assembled by call-site string concatenation. These limits follow YDB's
[database object naming rules](https://ydb.tech/docs/en/concepts/datamodel/cluster-namespace)
and [database limits](https://ydb.tech/docs/en/concepts/limits-ydb).

Trino 479 normalizes SQL identifiers to lowercase, including delimited
identifiers. YDB paths are case-sensitive. A lowercase Trino name may resolve to
one unique case-insensitive remote path; case-only collisions must fail with an
explicit ambiguous-name error rather than selecting an arbitrary object.

### Catalog provisioning (approved operator guidance)

Static catalog property files are the production default. For example,
`ydb_prod.properties` and `ydb_analytics.properties` contain separate YDB JDBC
URLs and expose `ydb_prod.default` and `ydb_analytics.default`. Trino 479 dynamic
catalog management can optionally create the same connector instances with
`CREATE CATALOG`, but it is an experimental Trino deployment feature and not
YDB database discovery by the connector. Sensitive catalog properties must not
be placed directly in SQL because the complete statement is logged and visible
in the Trino Web UI.

This is the approved deployment and operator contract. The verified federation
slice below did not load production catalog property files or execute SQL
`CREATE CATALOG`.

### Catalog federation

Federated reads qualify each source by catalog. Cross-catalog joins run in
Trino; join pushdown requires both tables to belong to the same catalog.
Trino 479 permits one autocommit statement to read from several catalogs and
write to one target catalog when the target operation is supported. This
read-many/write-one YDB topology provides neither a cross-catalog snapshot nor
a distributed commit.

Cross-catalog reads may run in an explicit transaction. YDB is a
single-statement-write connector: when YDB is the first or only write target,
Trino rejects the write before connector mutation with `Catalog only supports
writes using autocommit: <catalog>`. A read-only transaction or an earlier write
to another catalog can instead surface the corresponding read-only or
multi-catalog-write error.

### Implemented namespace slice (verified 2026-08-20)

This slice implements the following items:

1. Introduced one path parser/validator used by metadata and table operations.
2. Changed the synthetic schema from `ydb` to `default`.
3. Made `listTables` and `getTableHandle` honor the schema filter and preserve
   the complete relative YDB table path.
4. Mapped only genuine remote not-found results to an absent table handle and preserved
   other JDBC/YDB failures.
5. Added focused tests for root and nested tables, an unknown schema, path
   traversal, dot-prefixed paths, case-only ambiguity, and full-path quoting.
6. Made relation-comment metadata and opt-in bulk column metadata honor the
   virtual `default` schema, recursive paths, and case-only collision checks.
   Comment writes remain unsupported, and bulk column metadata remains opt-in.

This slice does not enable schema DDL capabilities or change capability flags.

The real-YDB `testNestedTablePathLongerThanSingleComponentLimit` passed in the
final full suite. It covers a complete relative path longer than 255 characters
whose components are each at most 255 characters, including listing, selection,
rename, and cleanup.

### Verified catalog federation (2026-08-20)

The JDK 25 GitHub Actions run at `619b33d` verified the federation slice with
six tests: five new public scenarios and the inherited naming convention. The
fixture uses two independent Docker YDB instances, both with database `/local`,
separate endpoints, and Trino catalogs `ydb` and `ydb_analytics`. It allocates
fixed container ports outside the standard Linux and macOS ephemeral ranges,
checks wildcard availability, and retries the complete container start at most
three times on a Docker host-port collision. The YDB test-helper gRPC proxy is
not used because its unshaded generated proto stubs are binary-incompatible
with the same proto class names embedded in `ydb-jdbc-driver-shaded`.

- catalog discovery, `default`/`USE` analysis, and explicit `Session` handling
  for unqualified access;
- same-name table isolation, a cross-catalog JOIN evaluated in Trino, and
  explicit cross-catalog reads;
- one autocommit statement that reads from both catalogs and writes to one;
- rejection of the first and only YDB write in an explicit cross-catalog
  transaction before connector mutation, with the exact error `Catalog only
  supports writes using autocommit: ydb_analytics` and an empty target table.

This verifies the stated connector behavior only. It does not provide a shared
snapshot or distributed commit, and it makes no production configuration or
capability declaration changes.

## P0 — validate the JDBC batch/key contract and bound MERGE resources

`YdbMergeSink` already calls JDBC `addBatch()` for DELETE, UPDATE, and INSERT.
Source inspection of the pinned YDB JDBC 2.3.18 driver shows that, with its
default prepare/auto-batch settings, each eligible non-empty `executeBatch()`
is represented as one typed `List<Struct>` parameter and one set-based YQL
request. The generated forms use
[`UPDATE ... ON`](https://ydb.tech/docs/en/yql/reference/syntax/update),
`DELETE ... ON`, or `INSERT ... SELECT` over
[`AS_TABLE($batch)`](https://ydb.tech/docs/en/yql/reference/syntax/select/from_as_table).
This is driver-owned batching; the connector must not duplicate that rewrite.
This request cardinality is source-derived; the current connector tests do not
instrument or count remote requests.

The rewrite requires the simple SQL shape recognized by the driver's
`YqlBatcher`, complete primary-key equality for UPDATE/DELETE,
`disablePrepareDataQuery=false`, `disableAutoPreparedBatches=false`, and no
query modifier that changes the recognized shape. In particular, the default
blank `query.comment-format` preserves batching. A pinned-driver parser test
also verifies that Trino's trailing block-comment shape preserves automatic
batch recognition for the connector's DELETE, UPDATE, and INSERT SQL. This does
not instrument or count real remote requests.

1. The focused MERGE contract test now uses a composite metadata primary key,
   a non-key leading visible column, and two cross-sibling rows covering both
   individual key components of the UPDATE and DELETE targets. It verifies
   that both operations address the complete physical key while
   update/delete/insert branches coexist.
2. Preserve the implemented transaction invariants: each attempt opens one
   connection, sets `autoCommit=false`, executes all operation groups, and
   commits once; write parallelism is limited to one and Trino query/task retry
   modes are rejected. Each connector retry opens fresh transactional state.
   Once `Connection.commit()` has been invoked, the connector never replays the
   attempt: a commit exception is reported as an unknown outcome after
   best-effort rollback and close. Retrying an unknown outcome and returning a
   definite result still requires an operation-id or staging/finalize design.
3. Direct UPDATE and MERGE now reject physical primary-key changes before
   remote mutation with `NOT_SUPPORTED`. Atomic delete+insert remains a
   possible future extension. See the
   [YQL UPDATE contract](https://ydb.tech/docs/en/yql/reference/syntax/update).
4. MERGE now reuses Trino's validated `write_batch_size` session property and
   executes each DELETE, UPDATE-case, and INSERT JDBC batch in row-bounded
   chunks without intermediate commits. It scans the original pages by phase
   instead of retaining operation-specific pages and position arrays, and
   releases the original pages after the terminal outcome. The complete input
   remains buffered through internal retries, but retained input is now bounded
   to 64 MB per sink by default through the configurable
   `merge.max-buffer-size`. Exceeding the limit fails before a connection is
   opened or YDB is mutated. `ConnectorMergeSink.storeMergedRows` returns
   `void`, so this is a fail-fast bound rather than asynchronous backpressure or
   spill-to-disk. The JDBC row limit is still not a byte or serialized-request
   limit.
5. In-memory lifecycle tests inject a retryable failure after one sub-batch and
   verify complete replay on a fresh connection, then inject a retryable commit
   failure and verify no replay, rollback/close cleanup, and preservation of
   the original exception. Mixed-operation coverage verifies phase ordering,
   composite row-id keys, original update-channel mapping, row-bounded chunks,
   one final commit, rejection of an unknown update case before remote mutation,
   the retained-memory boundary before remote mutation, successful-commit close
   failure handling, and suppression of rollback/close failures without
   replacing the original SQL failure.

**Exit criterion:** retain the green inherited `testMerge*` suite, cover the
physical composite-key and rejection contracts, and demonstrate the chosen
page/request limits with a bounded-memory production-scale benchmark. A safe
staging/finalize or operation-id design is required before claiming replay-safe
retries after uncertain commit outcomes.

### Merge-key slice validation (2026-08-20)

GitHub Actions run
[`32380080735`](https://github.com/ydb-platform/ydb-java-dialects/actions/runs/32380080735)
at `f798b6a` reported 338 tests, 0 failures, 0 errors, and 84 skipped. It
included Connector 288/80, Smoke 36/4, Federation 6/0, TablePath 5/0, and the
port allocator 3/0. This final run includes both cross-sibling composite-key
cases and verifies that neither operation can accidentally use only one key
component.

### MERGE sub-batch slice validation (2026-08-20)

A focused JDK 25 run of `TestYdbMergeSink` reported 5 tests, 0 failures,
0 errors, and 0 skipped without initializing Docker. It covers the
pre-commit fresh-transaction retry, ambiguous-commit no-replay boundary,
`write_batch_size` chunking without intermediate commits, mixed-operation
phase/key/channel mapping, and invalid-update-case preflight. This focused run
does not replace the inherited real-YDB MERGE suite or the full module suite.

## P1 — retry and transaction hardening

- Connector-owned MERGE retries use `StatusCode.isRetryable(false)` from the
  pinned SDK. A focused classification test freezes its current unconditional
  set: `ABORTED`, `UNAVAILABLE`, `OVERLOADED`, `BAD_SESSION`, `SESSION_BUSY`,
  and `CLIENT_RESOURCE_EXHAUSTED`. `TIMEOUT`, `UNDETERMINED`,
  `SESSION_EXPIRED`, transport failures, and other conditional statuses are
  not retried for non-idempotent writes. See
  [YDB SDK error handling](https://ydb.tech/docs/en/reference/ydb-sdk/error_handling).
- Removed the generic `BaseJdbcClient.execute` retry wrapper. That hook receives
  a caller-owned JDBC connection and cannot replace it, while `BAD_SESSION` and
  `SESSION_BUSY` explicitly require a new session. DDL failures now surface to
  Trino instead of replaying on the same connection; MERGE retains its separate
  fresh-connection transaction retry.
- Keep the JDBC `SessionPool.acquire` scheduler-rejection workaround limited to
  that provably pre-execution stack. Track it against the YDB JDBC driver and
  remove the connector workaround after upgrading to a fixed driver.
- YDB JDBC 2.3.18 connection-context caching has a close/register race under
  concurrent connections. The connector therefore defaults
  `cacheConnectionsInDriver` to `false`; an explicit JDBC URL option can
  override it. This trades connection reuse for correctness and may increase
  connection latency/load. Re-evaluate the default after upgrading to a driver
  with a verified cache-lifecycle fix.
- The adapter adds no retry around Trino 479 `JdbcPageSink`. That sink commits
  every full JDBC batch. The integration fixture explicitly enables
  `insert.non-transactional-insert.enabled=true`, so a failed test INSERT may
  leave already committed target rows; query/task retries remain disabled.
  Trino's default staging path previously generated `CREATE TABLE AS SELECT`,
  which cannot create a row-oriented YDB table and omitted YDB's mandatory
  primary key. The connector now creates a regular staging table with the
  source columns' physical JDBC type names/nullability and a collision-safe
  hidden `BigSerial` primary key. Trino still performs one final
  `INSERT ... SELECT` into the target and drops the staging table. A real-YDB
  atomicity test verifies that a duplicate-key failure in a staged insert does
  not partially mutate the target and that a following valid staged insert
  succeeds.
- Focused unit tests now cover status classification, interrupted backoff while
  preserving the final SQL failure as the cause, pre-commit connection cleanup,
  failure after commit, and suppression of rollback/close failures without
  replacing the original SQL failure.
- Exercise and tune the MERGE memory limit with a production-scale workload;
  the unit boundary is covered, but spill-to-disk is not implemented.

### Retry-classification slice validation (2026-08-20)

A focused JDK 25 run of `TestYdbRetryUtils` and `TestYdbMergeSink` reported 7
tests, 0 failures, 0 errors, and 0 skipped without initializing Docker. The
slice verifies the complete pinned-SDK unconditional status set, nested-cause
classification, and the existing fresh-connection MERGE lifecycle. It does not
replace the real-YDB or full module suite.

### Interrupted MERGE retry validation (2026-08-20)

A focused JDK 25 run of `TestYdbMergeSink` reported 6 tests, 0 failures, 0
errors, and 0 skipped without initializing Docker. The added case interrupts a
retry backoff after a retryable batch failure and verifies the interrupt flag,
the original SQL exception as the `TrinoException` cause, the interrupted
sleep as suppressed evidence, and rollback/close cleanup.

### Transactional INSERT staging slice (2026-08-20)

Official YQL documents that a primary key is mandatory for YDB tables and that
`CREATE TABLE AS SELECT` currently supports only column-oriented tables. Source
inspection of Trino 479 confirmed that its generic staging path emits a CTAS
without a primary key. A focused JDK 25 unit run of `TestYdbInsertStaging`
reported 1 test, 0 failures, 0 errors, and 0 skipped without initializing
Docker; it verifies full-path quoting, collision-safe staging-key generation,
physical remote type preservation, nullability, and requested column order.
`testTransactionalInsertStagingDoesNotPartiallyMutateTarget` compiles and is the
real-YDB gate: with batch size one, a later duplicate-key failure does not leave
an earlier row in the target, followed by a successful staged insert. The gate
passed as part of the complete local suite on 2026-08-24.

### Clean integration snapshot validation (2026-08-21)

The review branches were reconstructed from current `main` as one local
integration branch without the three internal `docs/plans` artifacts. JDK 25
compile succeeded. One focused run of `TestYdbTablePath`,
`TestNonEphemeralPortsGenerator`, `TestYdbMergeSink`, `TestYdbRetryUtils`, and
`TestYdbInsertStaging` reported 17 tests, 0 failures, 0 errors, and 0 skipped.
These tests did not initialize Docker. The real-YDB transactional INSERT gate
and complete module suite were still pending at that snapshot.

### Local readiness audit validation (2026-08-21)

A fresh JDK 25 no-Docker run after the readiness fixes reported 24 tests,
0 failures, 0 errors, and 0 skipped. It includes 10 MERGE lifecycle/resource
tests, six path tests, three port-allocation tests, two retry-classification
tests, and one test each for INSERT staging, date predicate control, and pinned
JDBC auto-batch parsing. The real-YDB focused gates and complete module suite
were still pending at that audit snapshot.

### Complete local validation (2026-08-24)

A JDK 25 `clean test` run with Testcontainers and Colima reported 364 tests,
0 failures, 0 errors, and 84 skipped, leaving 280 passed. It included Connector
291/80, Smoke 36/4, Federation 6/0, ColumnMappings 8/0, MergeSink 10/0,
TablePath 6/0, NonEphemeralPorts 3/0, RetryUtils 2/0, and one test each for
INSERT staging and pinned JDBC batching. This is a local compatibility result
for the unpinned YDB test-helper image that resolved on that run; it is not an
image-version compatibility baseline.

## P2 — remove remaining test debt

Audit every inherited-test override and every `hasBehavior` exception. For each
unsupported behavior, record:

1. the exact YDB/YQL or Trino limitation;
2. an authoritative documentation link or tracked upstream issue;
3. a focused negative test that proves the connector fails clearly.

The former false negative-date declaration has been removed: Trino-created
columns use `Date32`, while legacy YDB `Date` predicates outside 1970-01-01
through 2105-12-31 remain in Trino. CHAR is rejected with the focused inherited
contract (`Unsupported column type: char(3)`) because YDB has no fixed-width
string primitive and mapping it to `Text` would lose Trino padding semantics.
A YDB-native fixture replaces the former empty default-column INSERT override.

### Capability audit (Trino 479)

| Behavior group | Declaration and verified reason |
| --- | --- |
| schema DDL and cross-schema rename | false: `default` is a virtual, fixed root schema; focused namespace and federation tests cover the boundary |
| TRUNCATE | false by the target connector contract; the inherited unsupported branch verifies `NOT_SUPPORTED` |
| ARRAY, MAP, ROW | false: YDB container types cannot be table column types; inherited unsupported data-mapping branches cover writes ([YDB containers](https://ydb.tech/docs/en/yql/reference/types/containers)) |
| column/table comments and add-column comments | false: the YDB table DDL used by the connector has no Trino comment contract; inherited negative branches verify clear rejection |
| add-column position, rename column, set column type | false: current YDB column DDL documents add, option changes, and drop, but no `FIRST`/`AFTER`, rename-column, or type-change form ([changing columns](https://ydb.tech/docs/en/yql/reference/syntax/alter_table/columns)) |
| views and materialized views | false: although current YDB has `CREATE VIEW`, it requires YDB-specific definition/security handling that the connector does not model; no view SPI implementation is present ([CREATE VIEW](https://ydb.tech/docs/en/yql/reference/syntax/create-view)) |
| default column values | false: Trino 479 exposes one catalog capability across CREATE, ADD, INSERT, MERGE, SET, and DROP. A seven-test real-YDB audit produced 1 pass, 3 failures, and 3 errors: YDB rejects `DEFAULT NULL`, MERGE staging does not preserve omitted defaults, and Base JDBC implements neither SET nor DROP. The inherited `testCreateTableWithDefaultColumn` negative branch and the positive YDB-native insert fixture define the current boundary. YDB itself supports literal defaults on row tables ([CREATE TABLE](https://ydb.tech/docs/en/yql/reference/syntax/create_table)) |
| add NOT NULL column | false: real YDB rejects adding a non-null column without a default even to an empty table, while the inherited Trino contract requires that statement; the connector rejects it before remote mutation |

## Later capability work

- List/Dict/Struct mappings for Trino ARRAY/MAP/ROW;
- views, comments, rename column, and type changes after checking current YQL
  semantics;
- preserve omitted defaults through INSERT/MERGE staging before advertising
  Trino's coarse `DEFAULT_COLUMN_VALUE` capability.

## Validation ladder

Use JDK 25 and the Docker/Testcontainers environment documented in the root
`AGENTS.md`:

```bash
mvn -f ydb-trino-adapter/pom.xml -DskipTests compile
mvn -f ydb-trino-adapter/pom.xml -Dtest='TestYdbConnectorTest#testName' test
mvn -f ydb-trino-adapter/pom.xml -Dtest=TestYdbConnectorSmokeTest test
mvn --batch-mode --update-snapshots -f ydb-trino-adapter/pom.xml clean test
```

Always report the test count and skipped count. Do not disable Docker-backed
tests, weaken CI, or add a no-op override to obtain a green result.
