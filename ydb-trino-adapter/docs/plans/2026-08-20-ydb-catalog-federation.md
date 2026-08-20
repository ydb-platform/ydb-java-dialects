# YDB Catalog Federation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Verify that two statically configured Trino YDB catalogs remain isolated while supporting catalog discovery, the `default` schema UX, cross-catalog reads, one autocommit read-many/write-one statement, and the current explicit-transaction write rejection.

**Architecture:** Start two genuinely independent Docker-backed YDB helpers sequentially; both expose database `/local`, but each has a different endpoint. Build the existing `ydb` catalog against the first helper and add `ydb_analytics` through Trino 479 `QueryRunner.createCatalog`, reusing one test-only connector-property factory so both catalogs receive identical non-connection settings. Add an integration class only; no `src/main` code, connector capability, or namespace behavior changes belong in this slice.

**Tech Stack:** Java 25, Trino 479 JDBC SPI and testing framework, YDB JDBC 2.3.18, YDB SDK/test helper 2.3.13, JUnit 5.10.1, AssertJ 3.25.3, Maven, Testcontainers 1.20.0, Docker/Colima.

**Spec:** `ydb-trino-adapter/ROADMAP.md`, sections “Approved namespace and catalog contract”, “Catalog provisioning (approved operator guidance)”, and “Catalog federation”; this is a stacked follow-up to namespace PR #242.

## Global Constraints

- Implement on `codex/ydb-catalog-federation`, stacked on namespace PR #242; do not rebase the work onto a base that lacks PR #242's `default` schema and full-relative-path contract.
- One Trino catalog remains one connector instance configured for one YDB database through its own `connection-url` and credentials.
- The test must use two independent YDB instances. Both database paths are `/local`; their endpoints must differ.
- Do not substitute `/local/a` and `/local/b` directories for databases. A YDB directory is not a separately deployed database root.
- Expose only the virtual Trino schema `default`; a table name remains the full relative YDB path.
- Keep production code under `src/main` and connector capability flags unchanged.
- Broaden only test-support APIs needed to accept the pinned `YdbHelper` interface and share exact connector properties.
- Start the first YDB helper before constructing the second helper. The pinned non-isolated test resource allocates fixed equal host/container ports, so concurrent construction can choose colliding ports.
- Force `dockerReuse()` to `false` for both federation helpers, and close the Trino query runner before either YDB helper.
- Serialize this integration class with `@Execution(SAME_THREAD)` to bound Docker, YDB, JDBC pool, and Trino worker load.
- Do not test `USE` persistence by issuing `USE` and then making another call with the unchanged default `TestingTrinoClient` session. Trino 479's test client does not carry catalog/schema response state between independent `execute` calls.
- Exercise explicit transactions with `newTransaction().execute(session, callback)` and do not call a helper that marks the transaction as `singleStatement`; the latter would manufacture an autocommit context that a public explicit transaction does not have.
- The exact expected YDB write error is `Catalog only supports writes using autocommit: ydb_analytics`, and the target must remain unchanged.
- Tests may be written before or after support refactoring according to implementation risk; this plan does not require TDD.
- Use JDK 25 and the Docker/Testcontainers environment from the root `AGENTS.md`.
- Do not predict new Surefire run/skipped totals. Record counts only from fresh reports after each validation command.

## File map

- Modify `ydb-trino-adapter/src/test/java/tech/ydb/trino/YdbQueryRunner.java`: accept `YdbHelper` and produce one reusable property map for any test catalog.
- Create `ydb-trino-adapter/src/test/java/tech/ydb/trino/TestYdbCatalogFederation.java`: own both transient YDB helpers, the second Trino catalog, all federation assertions, and table cleanup.
- Leave `ydb-trino-adapter/src/main/**`, `pom.xml`, connector behavior flags, README, and ROADMAP unchanged in this slice. The executor reports verified evidence to the integrator; documentation state can be updated only from those results.

---

### Task 1: Generalize the test runner's YDB connection properties

**Files:**
- Modify: `ydb-trino-adapter/src/test/java/tech/ydb/trino/YdbQueryRunner.java:12-45`

**Interfaces:**
- Produces: `public static Builder builder(YdbHelper ydb)`; existing `YdbHelperExtension` callers remain source-compatible because the extension implements `YdbHelper`.
- Produces: package-visible `static Map<String, String> connectorProperties(YdbHelper ydb)` for additional test catalogs.
- Produces: package-visible `static String buildJdbcUrl(YdbHelper ydb)` for existing raw-JDBC tests.

- [ ] **Step 1: Replace the concrete extension type with the helper interface**

Replace the `YdbHelperExtension` import with:

```java
import tech.ydb.test.integration.YdbHelper;
```

Change both signatures without changing URL syntax:

```java
public static Builder builder(YdbHelper ydb)

static String buildJdbcUrl(YdbHelper ydb)
```

The URL must remain `jdbc:ydb:<grpc-or-grpcs>://<endpoint><database>?useQueryService=true&sessionPoolMaxSize=10`, with the existing optional token handling.

- [ ] **Step 2: Centralize the standard connector properties**

Add a package-visible immutable factory:

```java
static Map<String, String> connectorProperties(YdbHelper ydb)
{
    return ImmutableMap.of(
            "insert.non-transactional-insert.enabled", "true",
            "connection-url", buildJdbcUrl(ydb));
}
```

Make `builder` consume the same map rather than spelling the two properties a second time:

```java
public static Builder builder(YdbHelper ydb)
{
    Builder builder = new Builder();
    connectorProperties(ydb).forEach(builder::addConnectorProperty);
    return builder;
}
```

Do not add credential logging or expose the generated URL outside package-scoped test support.

- [ ] **Step 3: Compile test sources without starting Docker**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
  mvn -f ydb-trino-adapter/pom.xml -DskipTests test-compile
```

Expected outcome: `BUILD SUCCESS`; existing `YdbHelperExtension` call sites compile through the `YdbHelper` interface. No test count is produced by this command.

- [ ] **Step 4: Review and commit the test-support refactor**

Review `git diff --check` and confirm that only `YdbQueryRunner.java` changed. Commit it as:

```bash
git add ydb-trino-adapter/src/test/java/tech/ydb/trino/YdbQueryRunner.java
git commit -m "Share YDB test catalog properties"
```

---

### Task 2: Add two-database lifecycle and federated-read coverage

**Files:**
- Create: `ydb-trino-adapter/src/test/java/tech/ydb/trino/TestYdbCatalogFederation.java`

**Interfaces:**
- Consumes: `YdbQueryRunner.builder(YdbHelper)` and `YdbQueryRunner.connectorProperties(YdbHelper)` from Task 1.
- Consumes: Trino 479 `QueryRunner.createCatalog(String, String, Map<String, String>)` and `AbstractTestQueryFramework.newTransaction()`.
- Produces: one Docker-only, same-thread integration class with catalogs `ydb` and `ydb_analytics`.

- [ ] **Step 1: Create a Docker-only YDB environment**

Create the class with these imports and constants:

```java
import io.trino.Session;
import io.trino.testing.AbstractTestQueryFramework;
import io.trino.testing.DistributedQueryRunner;
import io.trino.testing.QueryRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import tech.ydb.test.integration.YdbEnvironment;
import tech.ydb.test.integration.YdbHelper;
import tech.ydb.test.integration.YdbHelperFactory;

import static io.trino.testing.TestingNames.randomNameSuffix;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
```

Use this structure:

```java
@Execution(SAME_THREAD)
public class TestYdbCatalogFederation
        extends AbstractTestQueryFramework
{
    private static final String PRIMARY_CATALOG = "ydb";
    private static final String SECONDARY_CATALOG = "ydb_analytics";

    private YdbHelper primaryYdb;
    private YdbHelper secondaryYdb;

    private static final class LocalDockerEnvironment
            extends YdbEnvironment
    {
        @Override
        public String ydbEndpoint()
        {
            return null;
        }

        @Override
        public String ydbDatabase()
        {
            return null;
        }

        @Override
        public String dockerDatabase()
        {
            return "/local";
        }

        @Override
        public boolean dockerReuse()
        {
            return false;
        }
    }
}
```

Returning `null` for the external endpoint/database forces `YdbHelperFactory.createYdbHelper` down the Docker path even if an unrelated external YDB is configured in the environment. Keep `disableIntegrationTests()` inherited so the standard opt-out still works.

- [ ] **Step 2: Start and own both helpers sequentially**

Add:

```java
private static YdbHelper startLocalYdb()
{
    YdbHelperFactory factory = YdbHelperFactory.createYdbHelper(new LocalDockerEnvironment());
    assumeTrue(factory.isEnabled(), "Docker-backed YDB is unavailable");
    return requireNonNull(factory.createHelper(), "YDB helper is disabled");
}
```

Implement `createQueryRunner` in this exact order:

1. Start `primaryYdb` completely.
2. Only then construct and start `secondaryYdb`.
3. Build the Trino runner from `primaryYdb` with one worker.
4. Add the second catalog with the same connector factory and the secondary property map.
5. Register both helpers with `closeAfterClass` before returning the runner. `AbstractTestQueryFramework` registers the returned runner afterward, so its LIFO closer shuts down Trino first, then secondary YDB, then primary YDB.

```java
@Override
protected QueryRunner createQueryRunner()
        throws Exception
{
    DistributedQueryRunner queryRunner = null;
    primaryYdb = startLocalYdb();
    try {
        secondaryYdb = startLocalYdb();
        queryRunner = YdbQueryRunner.builder(primaryYdb)
                .setWorkerCount(1)
                .build();
        queryRunner.createCatalog(
                SECONDARY_CATALOG,
                "ydb",
                YdbQueryRunner.connectorProperties(secondaryYdb));

        closeAfterClass(primaryYdb);
        closeAfterClass(secondaryYdb);
        return queryRunner;
    }
    catch (Exception | Error failure) {
        closeSuppressing(failure, queryRunner);
        closeSuppressing(failure, secondaryYdb);
        closeSuppressing(failure, primaryYdb);
        throw failure;
    }
}
```

Add the null-safe cleanup helper so a failed secondary start or catalog creation does not leak the first container:

```java
private static void closeSuppressing(Throwable failure, AutoCloseable resource)
{
    if (resource == null) {
        return;
    }
    try {
        resource.close();
    }
    catch (Throwable closeFailure) {
        failure.addSuppressed(closeFailure);
    }
}
```

- [ ] **Step 3: Add table naming and cleanup helpers**

Use only generated simple table names in this slice; nested/full-path quoting already has focused coverage in `TestYdbConnectorTest#testNestedTablePath`.

```java
private static String tableName(String catalog, String table)
{
    return "%s.%s.%s".formatted(catalog, YdbQueryRunner.DEFAULT_SCHEMA, table);
}

private AutoCloseable dropTableOnClose(String table)
{
    return () -> assertQuerySucceeds("DROP TABLE IF EXISTS " + table);
}

private void createMarkerTable(String table, String marker)
{
    assertUpdate("CREATE TABLE " + table + " (id bigint NOT NULL, marker varchar)");
    assertUpdate("INSERT INTO " + table + " (id, marker) VALUES (1, '" + marker + "')", 1);
}
```

The marker arguments are fixed test literals (`primary` and `analytics`), never user input. Register each cleanup resource before issuing its corresponding `CREATE TABLE`, so partially completed setup still drops every table that exists.

- [ ] **Step 4: Test catalog discovery, `USE`, and the effective default session**

Add `testCatalogDiscoveryAndUseDefaultSchema`:

```java
@Test
public void testCatalogDiscoveryAndUseDefaultSchema()
        throws Exception
{
    assertThat(primaryYdb.database()).isEqualTo("/local");
    assertThat(secondaryYdb.database()).isEqualTo("/local");
    assertThat(primaryYdb.endpoint()).isNotEqualTo(secondaryYdb.endpoint());
    assertThat(computeActual("SHOW CATALOGS").getOnlyColumnAsSet())
            .contains(PRIMARY_CATALOG, SECONDARY_CATALOG);

    assertQuerySucceeds("USE ydb.default");
    assertQuerySucceeds("USE ydb_analytics.default");
    assertQueryFails("USE ydb_analytics.missing", ".*Schema 'missing' does not exist.*");

    String table = "federation_use_" + randomNameSuffix();
    String analyticsTable = tableName(SECONDARY_CATALOG, table);
    try (AutoCloseable ignored = dropTableOnClose(analyticsTable)) {
        createMarkerTable(analyticsTable, "analytics");

        // TestingTrinoClient does not retain USE response state between execute calls.
        Session analyticsSession = Session.builder(getSession())
                .setCatalog(SECONDARY_CATALOG)
                .setSchema(YdbQueryRunner.DEFAULT_SCHEMA)
                .build();
        assertQuery(analyticsSession, "SELECT marker FROM " + table, "VALUES 'analytics'");
    }
}
```

Do not assert the complete `SHOW CATALOGS` set because the runner also installs `system` and `tpch`.

- [ ] **Step 5: Test same-name isolation**

Add `testSameNamedTablesAreIsolated`. Create the same relative table name in both catalogs, insert different marker values, then prove each fully qualified read returns only its own value:

```java
String table = "federation_isolation_" + randomNameSuffix();
String primaryTable = tableName(PRIMARY_CATALOG, table);
String analyticsTable = tableName(SECONDARY_CATALOG, table);
try (AutoCloseable ignoredPrimary = dropTableOnClose(primaryTable);
        AutoCloseable ignoredAnalytics = dropTableOnClose(analyticsTable)) {
    createMarkerTable(primaryTable, "primary");
    createMarkerTable(analyticsTable, "analytics");

    assertQuery("SELECT id, marker FROM " + primaryTable, "VALUES (BIGINT '1', 'primary')");
    assertQuery("SELECT id, marker FROM " + analyticsTable, "VALUES (BIGINT '1', 'analytics')");
}
```

If both catalogs accidentally point at the same YDB instance, the second `CREATE TABLE` fails instead of silently passing this test.

- [ ] **Step 6: Test a cross-catalog read join**

Add `testCrossCatalogReadJoin` with the same cleanup pattern. Insert `(1, 'primary')` into the primary source and `(1, 'analytics')` into the analytics source, then run:

```sql
SELECT p.id, p.marker, a.marker
FROM ydb.default.<primary-table> p
JOIN ydb_analytics.default.<analytics-table> a ON p.id = a.id
```

Assert exactly:

```sql
VALUES (BIGINT '1', 'primary', 'analytics')
```

The result is the contract. Do not require a connector-specific plan node: a join between different catalog handles is coordinated by Trino and cannot be pushed as one YDB query.

- [ ] **Step 7: Run the focused read methods**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn -f ydb-trino-adapter/pom.xml \
  -Dtest='TestYdbCatalogFederation#testCatalogDiscoveryAndUseDefaultSchema+testSameNamedTablesAreIsolated+testCrossCatalogReadJoin' test
```

Expected outcome: selected methods pass with zero failures and errors. Record actual Surefire run/skipped counts; do not insert an estimated count into ROADMAP.

- [ ] **Step 8: Review and commit the read-federation slice**

Confirm `git diff --check` is clean, no `src/main` file changed, both database paths are asserted as `/local`, and endpoints are asserted different. Commit as:

```bash
git add ydb-trino-adapter/src/test/java/tech/ydb/trino/TestYdbCatalogFederation.java
git commit -m "Test YDB cross-catalog reads"
```

---

### Task 3: Verify read-many/write-one and explicit-transaction rejection

**Files:**
- Modify: `ydb-trino-adapter/src/test/java/tech/ydb/trino/TestYdbCatalogFederation.java`

**Interfaces:**
- Consumes: the two catalogs, table helpers, and lifecycle from Task 2.
- Consumes: `newTransaction().execute(Session, Consumer<Session>)`, whose default context is a real explicit multi-statement transaction because `.singleStatement()` is not requested.
- Produces: verified current write contract without enabling `SUPPORTS_MULTI_STATEMENT_WRITES`.

- [ ] **Step 1: Add one autocommit read-many/write-one statement**

Add `testAutocommitReadManyWriteOne`. Create one marker source in each catalog and an empty output table in the primary catalog. Register all three cleanup lambdas before creating tables. Execute one Trino statement:

```sql
INSERT INTO ydb.default.<output> (id, marker)
SELECT p.id, p.marker || ':' || a.marker
FROM ydb.default.<primary-source> p
JOIN ydb_analytics.default.<analytics-source> a ON p.id = a.id
```

Assert an update count of `1`, then assert the primary target contains:

```sql
VALUES (BIGINT '1', 'primary:analytics')
```

This is one autocommit statement with multiple read catalogs and exactly one write catalog. Do not claim a shared YDB snapshot or distributed commit.

- [ ] **Step 2: Add an explicit cross-catalog read followed by a rejected YDB write**

Add `testExplicitTransactionReadsAndRejectsYdbWrite`. Create both sources and an empty target in `ydb_analytics`. Build the transaction's base session explicitly:

```java
Session transactionBase = Session.builder(getSession())
        .setCatalog(PRIMARY_CATALOG)
        .setSchema(YdbQueryRunner.DEFAULT_SCHEMA)
        .build();
```

Within `newTransaction().execute(transactionBase, transactionSession -> { ... })`:

1. Run the same fully qualified cross-catalog read join and assert its row.
2. Attempt `INSERT INTO <analytics-target> (id, marker) VALUES (2, 'must-not-commit')`.

Wrap the transaction call with:

```java
assertThatThrownBy(() -> newTransaction().execute(transactionBase, transactionSession -> {
    assertQuery(
            transactionSession,
            crossCatalogJoin,
            "VALUES (BIGINT '1', 'primary', 'analytics')");
    assertUpdate(
            transactionSession,
            "INSERT INTO " + analyticsTarget + " (id, marker) VALUES (2, 'must-not-commit')",
            1);
}))
        .hasMessageMatching("Catalog only supports writes using autocommit: ydb_analytics");
```

After the transaction aborts, assert:

```java
assertQuery("SELECT count(*) FROM " + analyticsTarget, "VALUES BIGINT '0'");
```

The write must be the first and only write attempt in the explicit transaction. Do not add a `MULTI_CATALOG_WRITE_CONFLICT` assertion: the current connector inherits `isSingleStatementWritesOnly() == true`, so the public transaction is rejected by Trino before connector mutation on its first YDB write.

- [ ] **Step 3: Run both write-contract methods**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn -f ydb-trino-adapter/pom.xml \
  -Dtest='TestYdbCatalogFederation#testAutocommitReadManyWriteOne+testExplicitTransactionReadsAndRejectsYdbWrite' test
```

Expected outcome: the autocommit statement writes exactly one row; the explicit transaction observes both read catalogs, reports the exact autocommit-only error, and leaves its target empty. Record actual run/skipped counts only after the command finishes.

- [ ] **Step 4: Run the whole new federation class**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn -f ydb-trino-adapter/pom.xml \
  -Dtest=TestYdbCatalogFederation test
```

Expected outcome: no failures or errors and no leaked transient YDB container. Although the plan defines five `@Test` methods, discovered/run/skipped totals remain unknown until Surefire reports them and may differ when Docker integration is disabled.

- [ ] **Step 5: Review and commit write semantics**

Check that the diff contains no production transaction changes, capability flag changes, fake `MULTI_CATALOG_WRITE_CONFLICT` setup, or assertion that federation provides atomicity. Commit as:

```bash
git add ydb-trino-adapter/src/test/java/tech/ydb/trino/TestYdbCatalogFederation.java
git commit -m "Test YDB federated write boundaries"
```

---

### Task 4: Run the validation ladder and prepare the local PR evidence

**Files:**
- Verify only: `ydb-trino-adapter/src/test/java/tech/ydb/trino/YdbQueryRunner.java`
- Verify only: `ydb-trino-adapter/src/test/java/tech/ydb/trino/TestYdbCatalogFederation.java`
- Do not modify: `ydb-trino-adapter/ROADMAP.md` until the integrator decides how to record the verified stacked result.

**Interfaces:**
- Consumes: completed Tasks 1-3 and namespace PR #242.
- Produces: exact compile/test evidence for review; it does not produce guessed counts or an upstream support claim.

- [ ] **Step 1: Compile with the CI JDK**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn -f ydb-trino-adapter/pom.xml -DskipTests compile
```

Expected outcome: `BUILD SUCCESS`.

- [ ] **Step 2: Re-run the focused federation class from a fresh report**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn -f ydb-trino-adapter/pom.xml -Dtest=TestYdbCatalogFederation test
```

Expected outcome: no failures or errors. Record passed, failed, error, and skipped counts from `ydb-trino-adapter/target/surefire-reports/`.

- [ ] **Step 3: Run the selected namespace and federation classes**

Do not run this concurrently with another Maven/Testcontainers suite:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn -f ydb-trino-adapter/pom.xml \
  -Dtest='TestYdbCatalogFederation,TestYdbConnectorTest,TestYdbConnectorSmokeTest,TestYdbTablePath' test
```

Expected outcome: no failures or errors. Record each class's exact run/skipped counts rather than deriving them from the pre-PR baseline.

- [ ] **Step 4: Run the CI-equivalent full suite**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn --batch-mode --update-snapshots -f ydb-trino-adapter/pom.xml clean test
```

Expected outcome: no failures or errors. Compare the command and JDK with `.github/workflows/ci-trino-adapter.yaml`; record exact passed, failed, error, and skipped counts plus any environment limitation.

- [ ] **Step 5: Audit lifecycle and scope after the full run**

Confirm all of the following before requesting review:

- `git diff --check` succeeds.
- `git status --short` lists only the two test-source files intended by this slice.
- Two catalog property maps contain different `connection-url` endpoints and database `/local`.
- The secondary helper was not created until the first helper had started.
- The full test process closed Trino before stopping both transient YDB instances.
- No YDB container created with `dockerReuse=false` remains after the federation class.
- No `src/main`, capability, README, ROADMAP, example, or unrelated module file changed.
- The final report distinguishes verified results from the absence of cross-catalog snapshot or distributed-commit guarantees.

- [ ] **Step 6: Prepare the stacked local PR handoff**

Report:

- dependency on namespace PR #242;
- changed files and commits;
- exact commands and Surefire counts for focused, selected, and full runs;
- whether Docker-backed tests were executed or skipped;
- the verified success of catalog discovery, `USE` analysis, explicit-session default selection, same-name isolation, cross-catalog join, autocommit read-many/write-one, and explicit transaction rejection;
- lifecycle/resource risks and the fact that no production connector behavior changed.

Do not open an upstream Trino PR from this slice. It is one stacked local adapter PR toward the larger readiness roadmap.

---

## Pinned API evidence for implementers

- Trino 479 `DistributedQueryRunner.createCatalog` accepts a catalog name, connector factory name, and independent property map and installs the catalog on the test cluster.
- Trino 479 `Connector.isSingleStatementWritesOnly()` defaults to `true`; `YdbConnector` inherits that behavior through `JdbcConnector`.
- Trino 479 `InMemoryTransactionManager` raises `AUTOCOMMIT_WRITE_CONFLICT` for the first write by a single-statement-only connector in an explicit transaction.
- `ydb-junit5-support:2.3.13` `YdbHelperFactory.createYdbHelper(YdbEnvironment)` can create independent Docker factories; each `DockerHelperFactory` owns one `YdbDockerContainer`.
- `ydb-tests-common:2.3.13` defaults `dockerDatabase()` to `/local`, and `YdbDockerContainer` uses three ports per instance. The plan overrides the database explicitly and disables reuse for deterministic ownership.
- YDB database paths are deployed database roots, while directories are schema objects inside one database; a directory must not be used to fake a second catalog boundary.
