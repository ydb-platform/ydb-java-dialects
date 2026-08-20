# YDB MERGE Physical-Key Contract Plan

**Goal:** Reject physical YDB primary-key changes before remote mutation and
verify that UPDATE/DELETE/INSERT MERGE operations address complete composite
keys even when the first visible column is not a key.

**Architecture:** Keep the current direct-to-target merge transaction. Resolve
physical keys once through JDBC metadata, then validate direct UPDATE
assignments in `YdbClient.update` and row-level UPDATE/MERGE cases in
`YdbClient.beginMerge`. Do not emulate a key change with delete+insert in this
slice. The pinned YDB JDBC 2.3.18 driver already rewrites eligible JDBC batches
to typed set-based YQL; connector-owned batching and resource bounds remain
separate follow-up work.

**Stack:** Trino 479, YDB JDBC 2.3.18, Java 25, Docker-backed real-YDB tests.

## Constraints

- Work only in `ydb-trino-adapter/` on `codex/ydb-merge-key-contract`, stacked
  on the catalog-federation branch.
- Preserve all UPDATE/MERGE capability declarations and inherited tests.
- Compare assignments with the ordered physical key metadata, never with the
  first visible column or a test-only logical key.
- Fail with Trino `NOT_SUPPORTED` before opening the merge transaction or
  executing direct UPDATE YQL; name every offending key column.
- Keep target contents unchanged after every rejected statement.
- Use raw YDB DDL for focused tests so the visible composite key is physical.
- Do not add another SQL/YQL batch rewriter or claim bounded memory/at-most-once
  commit semantics in this slice.

## Tasks

1. Add one `YdbClient` physical-key assignment validator and call it from both
   `update` and `beginMerge`.
2. Add a real-YDB composite-key MERGE test with a non-key leading column and
   update/delete/insert branches.
3. Add direct UPDATE and MERGE physical-key rejection assertions, including
   error code/message and full unchanged-table checks.
4. Correct `ROADMAP.md`: eligible merge batches are already set-based through
   pinned JDBC defaults; the remaining P0 debt is composite-key proof,
   unbounded memory/request size, and uncertain-commit replay/staging.
5. Run compile, focused tests, inherited UPDATE/MERGE coverage, the complete
   adapter suite, diff review, and a clean-worktree check before pushing.

## Validation

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -f ydb-trino-adapter/pom.xml -DskipTests compile

JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn -f ydb-trino-adapter/pom.xml \
  -Dtest='TestYdbConnectorTest#testMergeUsesCompleteCompositePrimaryKeyWithNonKeyFirstColumn+testPhysicalPrimaryKeyUpdatesAreRejectedWithoutMutation' test

JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn --batch-mode --update-snapshots -f ydb-trino-adapter/pom.xml clean test
```

Record only fresh passed/failed/skipped counts. A green result does not close
the later bounded-memory, staging/finalize, or retry-hardening work.
