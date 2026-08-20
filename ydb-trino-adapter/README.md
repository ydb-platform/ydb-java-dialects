# YDB Trino Adapter

План развития и покрытие Trino connector tests: [ROADMAP.md](ROADMAP.md).

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
`default` schema and does not discover YDB databases.

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

Catalogs may be joined, but the join is executed by Trino rather than pushed
down to YDB:

```sql
SELECT p.order_id, a.segment
FROM ydb_prod.default."sales/eu/orders" p
JOIN ydb_analytics.default.customer_segment a ON p.customer_id = a.customer_id;
```

The intended future federation form is a single statement that reads from
several catalogs and writes to one; it would have no distributed snapshot or
distributed commit. Explicit transactions containing a YDB write are currently
rejected because this connector supports autocommit-only writes. This connector
slice has not integration-tested two-catalog federation.

Trino 479 can alternatively enable the experimental deployment feature
`catalog.management=dynamic` and create the same catalog instances with
`CREATE CATALOG`. This is not YDB database discovery. Do not place secrets in a
`CREATE CATALOG` statement: Trino logs the complete statement and displays it
in the Web UI. Use static catalog files and secret references for production
credentials.

```sql
CREATE CATALOG ydb_analytics
USING ydb
WITH ("connection-url" = 'jdbc:ydb:grpcs://<endpoint>/<database>?token=<secret>');
```
