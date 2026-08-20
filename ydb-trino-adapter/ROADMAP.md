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
components longer than the 255-character YDB component limit. It does not impose
an artificial 255-character limit on the complete relative path. The validated
full path is quoted as one YQL identifier through the connector quoting helper.
It is not assembled by call-site string concatenation.

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
blank `query.comment-format` preserves batching; a configured remote-query
comment has not yet been verified.

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
4. Define and test page/request memory limits. The connector retains all input
   pages until `finish()`, then creates operation-specific pages while the
   driver retains the corresponding row structs and list parameter. No current
   bounded-memory or maximum-request-size claim is made.
5. In-memory lifecycle tests inject a retryable batch failure before commit and
   verify replay on a fresh connection, then inject a retryable commit failure
   and verify one connection, one commit invocation, no replay, rollback/close
   cleanup, and preservation of the original exception. Extend rollback-failure
   suppression coverage separately.

**Exit criterion:** retain the green inherited `testMerge*` suite, cover the
physical composite-key and rejection contracts, and demonstrate the chosen
page/request limits with a bounded-memory production-scale benchmark. A safe
staging/finalize or operation-id design is required before claiming replay-safe
retries after uncertain commit outcomes.

### Merge-key slice validation (2026-08-20)

GitHub Actions run
[`32378003537`](https://github.com/ydb-platform/ydb-java-dialects/actions/runs/32378003537)
at `33bfb8f` reported 338 tests, 0 failures, 0 errors, and 84 skipped. It
included Connector 288/80, Smoke 36/4, Federation 6/0, TablePath 5/0, and the
port allocator 3/0. That run verified the physical-key rejection contract and
the initial cross-sibling fixture. Final review added the complementary
cross-sibling needed to catch either operation using either individual key;
the required GitHub Actions check on PR #244 gates that final test-only change.

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
- Apply and verify the P0 memory/backpressure limits for buffered merge pages.

## P2 — remove remaining test debt

Audit every inherited-test override and every `hasBehavior` exception. For each
unsupported behavior, record:

1. the exact YDB/YQL or Trino limitation;
2. an authoritative documentation link or tracked upstream issue;
3. a focused negative test that proves the connector fails clearly.

The existing negative-date, CHAR, cast-pushdown, and default-column overrides
need this treatment. YDB `Date` starts at the Unix epoch; see
[primitive types](https://ydb.tech/docs/en/yql/reference/types/primitive).

## Later capability work

- List/Dict/Struct mappings for Trino ARRAY/MAP/ROW;
- views, comments, rename column, and type changes after checking current YQL
  semantics;
- transactional INSERT/staging instead of direct non-transactional writes.

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
