# YDB Trino Adapter

Minimal Trino JDBC connector for YDB. Built on `trino-base-jdbc` and uses the official YDB JDBC driver.

## Build

```bash
mvn -f ydb-trino-adapter/pom.xml -DskipTests package
```

## Local example with YDB and Trino

`examples/` contains a minimal Docker Compose setup that runs both Trino (479) and YDB locally.

1) Build the plugin and assemble the plugin directory:

```bash
mvn -f ydb-trino-adapter/pom.xml -DskipTests package
mvn -f ydb-trino-adapter/pom.xml -DskipTests dependency:copy-dependencies -DincludeScope=runtime

mkdir -p ydb-trino-adapter/examples/trino/plugin
cp ydb-trino-adapter/target/ydb-trino-adapter-0.1.0.jar ydb-trino-adapter/examples/trino/plugin/
cp ydb-trino-adapter/target/dependency/*.jar ydb-trino-adapter/examples/trino/plugin/
```

2) Start Trino + YDB:

```bash
cd ydb-trino-adapter/examples
docker compose up -d
```

3) Open Trino UI: `http://localhost:8080`

4) Connect to Trino:

```bash
docker exec -it ydb-trino-trino trino
```

Check schemas and tables:

```sql
SHOW SCHEMAS FROM ydb;
SHOW TABLES FROM ydb.default;
```

## Connector configuration

Catalog file: `examples/trino/etc/catalog/ydb.properties`:

```
connector.name=ydb
connection-url=jdbc:ydb:grpc://ydb-local:2136/local
```

If you need a token or service account, add parameters to the JDBC URL as supported by the YDB JDBC driver.
