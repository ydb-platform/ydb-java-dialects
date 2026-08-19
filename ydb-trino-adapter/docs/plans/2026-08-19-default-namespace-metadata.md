# Default Namespace Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose one virtual Trino schema named `default` and address every YDB table by its validated full path relative to the configured database root.

**Architecture:** Keep catalog/database selection in Trino catalog configuration. Add one small immutable path type that validates user table names and provides case-insensitive comparison keys. `YdbClient` remains responsible for JDBC metadata: it lists visible remote paths recursively, rejects ambiguous case-only matches, returns absent only for genuine missing tables, and stores the resolved remote path in the table handle.

**Tech Stack:** Java 25, Trino 479 JDBC SPI, YDB JDBC 2.3.18, YDB SDK/test helper 2.3.13, JUnit 5, AssertJ, Maven, Testcontainers.

**Spec:** `ydb-trino-adapter/ROADMAP.md`, section “Approved namespace and catalog contract”.

## Global Constraints

- Work only in `ydb-trino-adapter/`; do not change unrelated modules.
- One Trino catalog is bound to one YDB database by `connection-url` and credentials.
- Expose exactly one virtual schema named `default`; schema DDL stays unsupported.
- The table name is the complete YDB path relative to the configured database root.
- Never concatenate a user path directly into YQL outside the existing identifier quoting helpers.
- Reject absolute, empty, repeated-separator, trailing-separator, `.`, `..`, dot-prefixed/system, invalid-character, and over-255-character path components.
- Preserve remote case only after a unique case-insensitive metadata match; reject case-only collisions explicitly.
- Do not change connector capability flags in this slice.
- Tests may be added before or after implementation according to risk; this plan does not require TDD.
- Use JDK 25 and the Docker/Testcontainers settings from the root `AGENTS.md`.

---

### Task 1: Add the validated YDB table-path value type

**Files:**
- Create: `ydb-trino-adapter/src/main/java/tech/ydb/trino/YdbTablePath.java`
- Create: `ydb-trino-adapter/src/test/java/tech/ydb/trino/TestYdbTablePath.java`

**Interfaces:**
- Produces: `YdbTablePath.fromUserInput(String)` for strict user validation.
- Produces: `YdbTablePath.fromRemoteMetadata(String)` returning an empty optional for hidden/system paths that must not be published.
- Produces: `value()` for the validated relative path and `comparisonKey()` for `Locale.ROOT` case-insensitive matching.

- [ ] **Step 1: Implement the immutable path type**

Create a package-private record with this interface and validation structure:

```java
record YdbTablePath(String value)
{
    private static final int MAX_COMPONENT_LENGTH = 255;
    private static final Pattern COMPONENT = Pattern.compile("[A-Za-z0-9._-]+");

    static YdbTablePath fromUserInput(String value)
    {
        return parseUserInput(value);
    }

    static Optional<YdbTablePath> fromRemoteMetadata(String value)
    {
        return parse(value, false);
    }

    String comparisonKey()
    {
        return value.toLowerCase(Locale.ROOT);
    }
}
```

Implement both parsing paths through one private component validator using `split("/", -1)`. `parseUserInput` must throw `TrinoException(INVALID_ARGUMENTS, ...)` with the rejected path and a non-sensitive reason. `fromRemoteMetadata` must return `Optional.empty()` for a dot-prefixed component so connector/system paths are hidden, but must throw `JDBC_ERROR` if JDBC reports another structurally invalid table path.

- [ ] **Step 2: Add focused unit coverage**

Test these exact categories in `TestYdbTablePath`:

```java
assertThat(YdbTablePath.fromUserInput("orders").value()).isEqualTo("orders");
assertThat(YdbTablePath.fromUserInput("sales/eu/orders").value()).isEqualTo("sales/eu/orders");
assertThat(YdbTablePath.fromUserInput("sales.eu/orders-v2").comparisonKey())
        .isEqualTo("sales.eu/orders-v2");
```

Iterate over `""`, `"/orders"`, `"orders/"`, `"a//b"`, `"."`, `".."`, `"a/../b"`, `".sys/table"`, and `"a/$/b"` in one test; each must fail with `INVALID_ARGUMENTS`. Add a 255-character passing component and a 256-character failing component. Verify `fromRemoteMetadata(".sys/table")` is empty and malformed non-system metadata raises `JDBC_ERROR`.

- [ ] **Step 3: Run the path unit test**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
  mvn -f ydb-trino-adapter/pom.xml -Dtest=TestYdbTablePath test
```

Expected: `TestYdbTablePath` passes with zero skipped tests.

- [ ] **Step 4: Commit the value type**

```bash
git add ydb-trino-adapter/src/main/java/tech/ydb/trino/YdbTablePath.java \
        ydb-trino-adapter/src/test/java/tech/ydb/trino/TestYdbTablePath.java
git commit -m "Validate YDB table paths"
```

---

### Task 2: Make JDBC metadata schema-aware and path-aware

**Files:**
- Modify: `ydb-trino-adapter/src/main/java/tech/ydb/trino/YdbClient.java:127`
- Test: `ydb-trino-adapter/src/test/java/tech/ydb/trino/TestYdbConnectorTest.java`

**Interfaces:**
- Consumes: `YdbTablePath.fromUserInput`, `fromRemoteMetadata`, `value`, and `comparisonKey` from Task 1.
- Produces: package-visible `YdbClient.DEFAULT_SCHEMA` with value `default` for runner/tests.
- Produces: `listRemoteTablePaths(Connection)` and `resolveRemoteTablePath(Connection, YdbTablePath)` as private JDBC metadata helpers.

- [ ] **Step 1: Replace the synthetic schema constant**

Replace `private static final String YDB_SCHEMA = "ydb"` with:

```java
static final String DEFAULT_SCHEMA = "default";
```

Use `DEFAULT_SCHEMA` for schema listing, logical table handles, and primary-key lookup fallbacks.

- [ ] **Step 2: Centralize recursive table discovery**

Add a helper that owns the metadata result lifecycle:

```java
private List<YdbTablePath> listRemoteTablePaths(Connection connection)
        throws SQLException
{
    ImmutableList.Builder<YdbTablePath> paths = ImmutableList.builder();
    try (ResultSet resultSet = getTables(connection, Optional.empty(), Optional.empty())) {
        while (resultSet.next()) {
            YdbTablePath.fromRemoteMetadata(resultSet.getString("TABLE_NAME"))
                    .ifPresent(paths::add);
        }
    }
    return paths.build();
}
```

Before returning public table names, group paths by `comparisonKey()`. Throw `TrinoException(AMBIGUOUS_NAME, ...)` when a group has more than one distinct remote value. Do not publish an arbitrary winner.

- [ ] **Step 3: Honor the schema filter in table listing**

Change `getTableNames` so a non-`default` schema returns `ImmutableList.of()` before opening a connection. For `Optional.empty()` and `Optional.of("default")`, list all visible paths and return `new SchemaTableName(DEFAULT_SCHEMA, path.value())`.

- [ ] **Step 4: Resolve table handles against actual remote metadata**

Change `getTableHandle` to return empty immediately for another schema, parse the requested path, match recursively listed remote paths by `comparisonKey()`, and use the actual remote spelling for one match. Return empty for zero matches and `AMBIGUOUS_NAME` for multiple matches. Store the requested logical name in schema `default` and the resolved path in a schema-less `RemoteTableName`.

Remove the `getColumns(...).next()` existence probe and the blanket `catch (SQLException) { return Optional.empty(); }`. Wrap metadata failures as `new TrinoException(JDBC_ERROR, "Failed to resolve YDB table " + schemaTableName, e)` and retain the cause.

- [ ] **Step 5: Validate names before producing YQL or remote names**

Change `toRemoteTableName` to call `YdbTablePath.fromUserInput(schemaTableName.getTableName())` and use its `value()`. Make `quoted(catalog, schema, table)` validate `table` the same way before quoting the complete path as one identifier; this covers base-JDBC CREATE operations. Validate `newTableName.getTableName()` in the public `renameTable` override before delegating to the base client. Existing handle-based DML continues to quote the already resolved remote path.

- [ ] **Step 6: Add connector-level schema assertions**

Add `testDefaultSchemaContract` to `TestYdbConnectorTest`. Assert `SHOW SCHEMAS FROM ydb` contains `default` and not `ydb`, `SHOW TABLES FROM ydb.default` contains `orders`, and `SELECT * FROM ydb.missing.orders` fails because the schema does not exist.

- [ ] **Step 7: Run metadata-focused tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn -f ydb-trino-adapter/pom.xml \
  -Dtest='TestYdbConnectorTest#testShowCreateSchema+testCreateTableSchemaNotFound+testDefaultSchemaContract' test
```

Expected: all selected methods pass; schema creation remains unsupported.

- [ ] **Step 8: Commit metadata behavior**

```bash
git add ydb-trino-adapter/src/main/java/tech/ydb/trino/YdbClient.java \
        ydb-trino-adapter/src/test/java/tech/ydb/trino/TestYdbConnectorTest.java
git commit -m "Expose YDB tables through default schema"
```

---

### Task 3: Exercise nested paths and path safety against real YDB

**Files:**
- Modify: `ydb-trino-adapter/src/test/java/tech/ydb/trino/YdbQueryRunner.java:18`
- Modify: `ydb-trino-adapter/src/test/java/tech/ydb/trino/TestYdbConnectorTest.java`

**Interfaces:**
- Consumes: `YdbClient.DEFAULT_SCHEMA` and metadata behavior from Task 2.
- Produces: package-visible `YdbQueryRunner.buildJdbcUrl(YdbHelperExtension)` for raw YDB setup where Trino cannot create a directory.

- [ ] **Step 1: Use `default` in the test session**

Replace `TPCH_SCHEMA = "ydb"` with `public static final String DEFAULT_SCHEMA = YdbClient.DEFAULT_SCHEMA`, build the `DistributedQueryRunner` session with it, and make `buildJdbcUrl` package-visible.

- [ ] **Step 2: Add directory lifecycle helpers**

Use `SchemeClient.newClient(ydb.createTransport()).build()` and absolute paths under `ydb.database()`:

```java
private static void createDirectory(String relativePath)
{
    try (SchemeClient client = SchemeClient.newClient(ydb.createTransport()).build()) {
        client.makeDirectories(ydb.database() + "/" + relativePath).join().expectSuccess();
    }
}
```

Add cleanup that removes leaf directories after their tables are dropped. Use a random suffix for every test path.

- [ ] **Step 3: Add nested-table round-trip coverage**

Create a two-level directory and then `CREATE TABLE "namespace_<suffix>/eu/orders" (id bigint NOT NULL)` through Trino. Verify `SHOW TABLES`, `SELECT`, and `SHOW CREATE TABLE` use the complete relative path. Drop the table and directories in `finally`.

- [ ] **Step 4: Add traversal and hidden-path negative coverage**

For each of `"/orders"`, `"a//orders"`, `"a/../orders"`, and `".sys/orders"`, execute a fully qualified `SELECT` and assert an actionable `Invalid YDB table path` failure. The test must prove that `a/../orders` is rejected even though YDB normalizes it inside a database.

- [ ] **Step 5: Add case-only ambiguity coverage**

Create `Case_<suffix>/Orders` and `case_<suffix>/orders` directly through YDB JDBC using `YdbQueryRunner.buildJdbcUrl(ydb)` and try-with-resources:

```java
try (Connection connection = DriverManager.getConnection(YdbQueryRunner.buildJdbcUrl(ydb));
        Statement statement = connection.createStatement()) {
    statement.execute("CREATE TABLE `" + remotePath + "` (id Int64 NOT NULL, PRIMARY KEY (id))");
}
```

The suffix is generated by the test and contains only safe identifier characters. Query the lowercase Trino path and assert an ambiguity failure naming both remote paths. Drop both tables through raw JDBC and both directories through `SchemeClient` in `finally`.

- [ ] **Step 6: Run focused path tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn -f ydb-trino-adapter/pom.xml \
  -Dtest='TestYdbConnectorTest#testNestedTablePath+testInvalidTablePaths+testCaseOnlyTablePathAmbiguity' test
```

Expected: all three methods pass with zero skips.

- [ ] **Step 7: Commit real-YDB coverage**

```bash
git add ydb-trino-adapter/src/test/java/tech/ydb/trino/YdbQueryRunner.java \
        ydb-trino-adapter/src/test/java/tech/ydb/trino/TestYdbConnectorTest.java
git commit -m "Test nested YDB table paths"
```

---

### Task 4: Update operator examples and verify the slice

**Files:**
- Modify: `ydb-trino-adapter/README.md`
- Modify: `ydb-trino-adapter/examples/trino/etc/catalog/ydb.properties`
- Modify only with verified counts: `ydb-trino-adapter/ROADMAP.md`

**Interfaces:**
- Consumes: the completed single-`default`-schema behavior from Tasks 1–3.
- Produces: operator examples for one catalog per database and full-path table names.

- [ ] **Step 1: Document provisioning and query UX**

Add concise static catalog examples for two databases with placeholder URLs and secret references. Document `USE ydb_prod.default`, a nested table query, and a cross-catalog join. Describe `catalog.management=dynamic` plus `CREATE CATALOG` as an optional experimental Trino deployment mechanism and warn that complete statements are logged. State that cross-catalog joins execute in Trino, read-many/write-one has no distributed snapshot/commit, and Trino 479 rejects multi-catalog writes.

- [ ] **Step 2: Compile with the CI JDK**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn -f ydb-trino-adapter/pom.xml -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run connector and smoke classes**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn -f ydb-trino-adapter/pom.xml \
  -Dtest='TestYdbConnectorTest,TestYdbConnectorSmokeTest,TestYdbTablePath' test
```

Expected: no failures or errors. Record exact run/skipped counts from Surefire.

- [ ] **Step 4: Run the CI-equivalent full suite**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
  mvn --batch-mode --update-snapshots -f ydb-trino-adapter/pom.xml clean test
```

Expected: no failures or errors. Compare with `.github/workflows/ci-trino-adapter.yaml` and report passed, failed, and skipped counts.

- [ ] **Step 5: Update only verified roadmap facts and commit**

Append exact local counts and observed limitations to `ROADMAP.md`; do not claim federation coverage until a second catalog is exercised. Commit README, example config, and verified roadmap results as `Document YDB catalog namespace usage`.

---

## Small-PR sequence after this slice

Each later subsystem gets its own reviewed implementation plan and branch from the then-current `main`:

1. Two-catalog YDB federation and dynamic/static provisioning integration coverage.
2. Set-based bounded MERGE batches and benchmark instrumentation.
3. MERGE transaction ownership, physical-primary-key update behavior, composite keys, and rollback/retry tests.
4. Retry classification, driver-workaround audit, post-commit INSERT failure tests, and backpressure accounting.
5. `hasBehavior` and inherited-override audit with focused negative tests.
6. ARRAY/MAP/ROW mappings, followed by views/comments/column evolution as separate capability PRs.
7. Packaging, security documentation, release validation, and final upstream-readiness review.

No upstream Trino pull request is opened until every completed capability has passed its focused tests and the full `ydb-trino-adapter` suite.
