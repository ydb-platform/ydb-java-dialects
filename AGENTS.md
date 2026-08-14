# AGENTS.md

## Repository purpose

This repository contains independent Java integrations for YDB: ORM dialects,
migration-tool extensions, retry helpers, and the Trino JDBC adapter. Preserve
the semantics of both the upstream framework and YDB; passing tests by claiming
unsupported behavior is not an acceptable substitute for implementing it.

These instructions apply to the whole repository. A more specific `AGENTS.md`
in a subdirectory takes precedence if one is added later.

## Repository layout

- Each top-level integration directory is an independent Maven project. There
  is no root aggregator `pom.xml`; run Maven with that module's `pom.xml`.
- `ydb-trino-adapter/` contains the Trino JDBC connector and is the primary
  module for Trino-to-YDB work.
- Module documentation belongs next to the module (`README.md`, `ROADMAP.md`,
  and release notes where present).
- GitHub Actions workflows under `.github/workflows/` are the source of truth
  for CI Java versions and build commands.

Do not reformat, build, or change unrelated modules unless the task requires
it. Before editing, inspect `git status --short --branch`; untracked and modified
files may belong to the user.

## Agent collaboration

Use the primary agent as the architect and integrator for substantial work.
Delegate bounded, independently verifiable investigations to subagents—for
example, separate YQL semantics, Trino test-contract, retry/transaction, and CI
failure audits—so one context does not need to retain every raw log and source
detail.

- Give each subagent an explicit scope, expected evidence, and read/write
  ownership. Avoid concurrent edits to the same file.
- Subagents report commands, relevant failures, documentation links, and a
  concrete recommendation; they do not declare the whole task complete.
- The primary agent reconciles conflicting recommendations, reviews every
  integrated diff, runs the cross-cutting tests, and owns the final support
  claim and roadmap.
- Keep architecture and capability decisions centralized. Parallelism is for
  evidence gathering and bounded implementation, not independent incompatible
  designs.

## Authoritative references

- YQL syntax: https://ydb.tech/docs/ru/yql/reference/syntax/
- YDB SDK error handling and retry classification:
  https://ydb.tech/docs/ru/reference/ydb-sdk/error_handling
- Trino SPI and JDBC behavior: use the source and tests for the exact Trino
  version declared in the module `pom.xml`.
- YDB JDBC and SDK behavior: use the versions declared in the module `pom.xml`.

Do not infer SQL support from another database. Check current YQL syntax and,
where semantics matter, verify the behavior against a real YDB instance.

## Java conventions

- Use the JDK release declared by the module and its CI workflow. The Trino
  adapter currently requires JDK 25.
- Follow the style of the surrounding source: four-space indentation, explicit
  imports, descriptive names, and braces consistent with the existing class.
- Do not add wildcard imports, empty catch blocks, blanket
  `@SuppressWarnings("all")`, or comments that merely repeat the code.
- Prefer small, direct changes and existing framework APIs over new wrappers.
- Preserve exception causes. Include actionable context without credentials,
  tokens, full connection strings, or row data that may be sensitive.
- Keep public behavior and resource ownership visible. Close JDBC resources
  with try-with-resources whenever possible.

## YDB and YQL rules

- Quote identifiers through the connector's quoting helpers. Never concatenate
  user values into YQL; bind values through `PreparedStatement`/Trino query
  parameters.
- Treat YDB paths, Trino schemas, and SQL catalogs as different concepts. Do
  not introduce schema-to-path behavior without an explicit design decision.
- Account for YDB type semantics instead of assuming ANSI SQL equivalence,
  especially for unsigned integers, `Utf8`/`Text`, dates, optional values,
  container types, and floating-point values.
- Push down an expression only when its YQL translation preserves Trino
  semantics. Otherwise return the appropriate unsupported/no-pushdown result
  and let Trino evaluate it.
- Do not derive a second SQL statement by regex-rewriting generated SQL.
  Construct it through `QueryBuilder` or a dedicated structured helper so that
  quoting, projections, predicates, and parameter order remain correct.

## Trino connector contracts

For `ydb-trino-adapter` changes, keep these layers separate:

- `YdbClient` owns JDBC/YQL translation, metadata, supported operations, and
  pushdown decisions.
- connector/module classes own dependency wiring and advertised capabilities.
- page sinks own write buffering and the lifecycle of insert/merge attempts.
- tests derived from Trino's `BaseConnectorTest` and
  `BaseConnectorSmokeTest` define the externally visible contract.

Capability declarations must tell the truth:

- Return `true` only when the operation and its required edge cases work.
- Do not disable a behavior merely to hide a regression.
- Unsupported behavior may be excluded through the Trino behavior mechanism
  or, when the upstream suite offers no suitable flag, through an explicit
  no-op test override. The override must name the concrete YDB/YQL or Trino
  planner limitation and link to authoritative documentation or a tracked
  upstream issue. A bare override used only to make CI green is not acceptable.
- When enabling a capability, run the complete group of inherited tests that
  the capability unlocks, not just the first happy-path test.

For UPDATE, DELETE, and MERGE:

- Preserve Trino's column ordering, primary-key mapping, nullability, affected
  row-count contract, and type conversions.
- Do not assume the first physical column is a primary key. Use JDBC metadata
  or an explicit test-table contract, and preserve composite-key order.
- A retry attempt must use a fresh valid transactional state. Roll back and
  close failed attempts before retrying; never retry non-transient errors.
- Retry only documented transient YDB status codes, with bounded backoff and
  jitter. Preserve interruption and the final SQL exception as the cause.
- A non-status client failure may be retried only when the connector can prove
  that no statement reached YDB; document the exact driver path and keep the
  classification narrower than the exception type alone.
- Make transaction ownership explicit before changing auto-commit, committing,
  or rolling back a connection supplied by Trino.
- YQL `UPDATE` cannot change primary-key columns:
  https://ydb.tech/docs/ru/yql/reference/syntax/update. Implement Trino updates
  of a physical YDB key as an atomic delete/insert row change, or reject that
  concrete statement with `NOT_SUPPORTED` and cite this limitation. Do not let
  it fail later as an opaque JDBC error.
- Prefer YQL `UPDATE ... RETURNING` / `DELETE ... RETURNING` when an affected
  row count is required. A separate `COUNT(*)` followed by DML is not atomic and
  must not be presented as an exact count under concurrent writes.

## Test workflow

Start with the narrowest relevant check, then widen it. From the repository
root, the Trino adapter commands are:

```bash
# macOS with Colima: use the CI JDK and expose its socket to Testcontainers
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn -f ydb-trino-adapter/pom.xml -DskipTests compile

JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn -f ydb-trino-adapter/pom.xml \
  -Dtest='TestYdbConnectorTest#testName' test

JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn -f ydb-trino-adapter/pom.xml clean test
```

On other systems, set `JAVA_HOME` to a JDK 25 installation using the platform's
normal mechanism. The integration suite starts YDB through the JUnit YDB helper
and requires a working Docker daemon. It may download Maven dependencies and a
container image on the first run. With Colima, setting only the Docker CLI
context is insufficient for Testcontainers 1.20; pass the socket variables
shown above and leave `TESTCONTAINERS_HOST_OVERRIDE` unset.

Use `ydb-trino-adapter/target/surefire-reports/` for exact failures. When a
failure mutates shared fixture data, rerun it in isolation before diagnosing
later failures: a failed write test can contaminate `nation`, `orders`, or
another table and produce misleading predicate/aggregation failures.

Before declaring a Trino adapter change complete:

1. Compile with the CI JDK.
2. Run every directly affected test method or class.
3. Run the full `ydb-trino-adapter` test suite.
4. Compare local results with `.github/workflows/ci-trino-adapter.yaml`.
5. Report passed, failed, and skipped counts plus any environment limitation.

For other modules, mirror the same progression with their own `pom.xml` and CI
workflow. Do not claim repository-wide success after testing only one module.

## Documentation and delivery

- Update the module README or roadmap when support, limitations, prerequisites,
  or operator behavior changes.
- Keep commits scoped and do not stage user files unrelated to the task.
- In the final handoff, list changed files, exact validation commands, remaining
  failures, and any semantic or operational risk. Distinguish verified facts
  from proposed follow-up work.
