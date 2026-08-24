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

## Extended temporal types

The connector enables the YDB JDBC 2.3.18 option
`forceSignedDatetimes=true` for every connection. This makes the driver's wide
temporal bindings available by default. Do not override this option to `false`
in `connection-url`; URL options take precedence over the connection properties
supplied by the connector.

New Trino `date` columns are created as YDB `Date32`, and new Trino
`timestamp(6)` columns are created as `Timestamp64`. Existing YDB temporal
columns remain readable: `Date` and `Date32` map to Trino `date`, `Datetime` and
`Datetime64` map to `timestamp(0)`, and `Timestamp` and `Timestamp64` map to
`timestamp(6)`. The signed types preserve values before 1970; `Timestamp64`
preserves microseconds. Predicates outside the physical YDB type's range remain
in Trino instead of being bound to JDBC.

The option is defined by the pinned driver in
[`YdbOperationProperties`](https://github.com/ydb-platform/ydb-jdbc-driver/blob/v2.3.18/jdbc/src/main/java/tech/ydb/jdbc/settings/YdbOperationProperties.java#L60-L62).
The YDB type name is `Timestamp64` (not `Timestampt64`); see the
[primitive type reference](https://ydb.tech/docs/en/yql/reference/types/primitive).

Atomic MERGE buffers input so a safe pre-commit retry can replay the complete
operation while preserving the global delete/update/insert phase order. Each
MERGE sink is limited to 64 MB of retained input by default. Operators may tune
the finite limit per catalog:

```properties
merge.max-buffer-size=128MB
```

If the limit is exceeded, the connector fails the MERGE before opening its YDB
connection or mutating the target. Trino 479's `ConnectorMergeSink` input method
does not expose asynchronous backpressure, so this is a fail-fast bound rather
than spill-to-disk.

A table name is the complete YDB path relative to that catalog's configured
database root. Select the only schema, then quote a nested path as one Trino
identifier:

```sql
USE ydb_prod.default;
SELECT * FROM "sales/eu/orders";
```

Each YDB path component is limited to 255 characters, and a relative object
path may contain at most 32 components. The connector does not apply the
component-length limit to the complete relative path, so a deeper path remains
valid within that hierarchy limit when every component is valid.
See YDB's [database object naming rules](https://ydb.tech/docs/en/concepts/datamodel/cluster-namespace)
and [database limits](https://ydb.tech/docs/en/concepts/limits-ydb).

Catalogs may be joined, but the join is executed by Trino rather than pushed
down to YDB:

```sql
SELECT p.order_id, a.segment
FROM ydb_prod.default."sales/eu/orders" p
JOIN ydb_analytics.default.customer_segment a ON p.customer_id = a.customer_id;
```

Trino 479 permits one autocommit statement to read from several catalogs and
write to one target catalog. The integration fixture covers this topology with
two independent YDB instances. It does not provide a cross-catalog snapshot or
distributed commit.

Cross-catalog reads may also run in an explicit transaction. YDB is a
single-statement-write connector: when YDB is the first or only write target,
Trino rejects the write before connector mutation with `Catalog only supports
writes using autocommit: <catalog>`. A read-only transaction or an earlier write
to another catalog can instead produce the corresponding read-only or
multi-catalog-write error.

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
