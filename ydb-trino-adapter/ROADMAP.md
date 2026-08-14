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

The full smoke class currently passes: 36 tests run, 0 failed, 0 errors, 4
skipped. The inherited connector suite is not fully green yet. The main measured
blocker is `testMergeLarge`: its one-million-row MERGE still ran after 11 minutes
and was stopped manually. It must not be replaced with an empty override.

## P0 — finish MERGE correctness and performance

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

**Exit criterion:** every inherited `testMerge*` test, including
`testMergeLarge`, runs without an override and within the GitHub Actions budget.

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

- schema-as-YDB-path design for CREATE/DROP/RENAME SCHEMA;
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
