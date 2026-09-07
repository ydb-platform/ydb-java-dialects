# YDB Trino Adapter

План развития и покрытие Trino connector tests: [ROADMAP.md](ROADMAP.md).

## Совместимость

Адаптер собирается для Trino 483 и требует 64-битную Java 25 версии 25.0.1
или новее в линейке Java 25. Версия Java 25 задана в `pom.xml` и в CI workflow
модуля.

# Инструкция по сборке

```bash

mvn -f pom.xml -DskipTests package
mvn -f pom.xml -DskipTests dependency:copy-dependencies -DincludeScope=runtime

mkdir -p docker/trino/plugin
cp target/ydb-trino-0.1.0.jar docker/trino/plugin
cp target/dependency/*.jar docker/trino/plugin

cd docker
docker-compose down
docker-compose up -d
```

## Запуск Trino CLI

```bash


docker exec -it ydb-trino trino
```

## YDB catalogs and table paths

Configure one static Trino catalog for each YDB database. A catalog is a
connection and security boundary; the connector exposes only the virtual
`default` schema and does not discover YDB databases. This replaces the former
virtual `ydb` schema: update SQL, saved queries and grants from
`catalog.ydb.table` to `catalog.default.table`. Directories are part of the table
name, never separate Trino schemas.

For example, deploy these two files under Trino's `etc/catalog/` directory.
The environment values must be injected from the deployment secret manager;
they are placeholders, not credentials to copy into source control.

`ydb_prod.properties`:

```properties
connector.name=ydb
connection-url=${ENV:YDB_PROD_JDBC_URL}
```

`ydb_analytics.properties`:

```properties
connector.name=ydb
connection-url=${ENV:YDB_ANALYTICS_JDBC_URL}
```

For example, `YDB_PROD_JDBC_URL` is a secret reference resolving to a complete
JDBC URL for the production database, including any authentication material.
Never store a real token or credential in a catalog file. The bundled
[`examples/trino/etc/catalog/ydb.properties`](examples/trino/etc/catalog/ydb.properties)
is the working local single-database example mounted by
`examples/docker-compose.yml`; it intentionally points at `ydb-local`. For
production, create `ydb_prod.properties` and a separate analytics catalog file
using the secret references above.

A table name is the complete YDB path relative to that catalog's configured
database root. Select the only schema, then quote a nested path as one Trino
identifier:

```sql
USE ydb_prod.default;
SELECT * FROM "sales/eu/orders";
```

Each YDB path component is limited to 255 characters. YDB also enforces its
configured path-depth limit (32 by default), counting the database path: with
`/local`, 31 relative components fit the default limit. The connector rejects
relative paths exceeding 32 components and does not apply the 255-character
component limit to the complete relative path. See YDB's
[database object naming rules](https://ydb.tech/docs/en/concepts/datamodel/cluster-namespace)
and [database limits](https://ydb.tech/docs/en/concepts/limits-ydb).

Trino lowercases identifiers while YDB paths are case-sensitive. A name resolves
to the one remote path that matches it case-insensitively; if two YDB objects
differ only by case, the connector fails with an explicit ambiguous-name error
instead of picking one. Reading or dropping a name outside the exposed namespace
(absolute path, empty or `.`-prefixed component, `..`) reports that the table
does not exist; creating or renaming to such a name fails with an
`Invalid YDB table path` error before any YQL is generated.

Dot-prefixed tables and directories are hidden by connector policy, including
ordinary dot-prefixed tables that YDB itself permits. CREATE, CTAS and RENAME
resolve each existing parent directory case-insensitively and retain its remote
spelling. Missing parents, non-directory parents and case-only directory
collisions fail explicitly; the connector does not create directories.

The JDBC `usePrefixPath` option is rejected because it changes metadata and YQL
resolution away from the configured database root. The JDBC driver's effective
`database` configuration (including its URL query parameter) selects that static
root. See [YQL TablePathPrefix](https://ydb.tech/docs/en/yql/reference/syntax/pragma#tablepathprefix).

In Trino 483, an exact-name `information_schema.columns` lookup suppresses remote
metadata errors and can return no rows for an ambiguous name. Direct table
access and broad bulk metadata enumeration report the ambiguity. Concurrent
external namespace changes are not locked by metadata resolution; later access
revalidates names, while YDB enforces exact-name DDL conflicts.
