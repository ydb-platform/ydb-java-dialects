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

Each catalog connects to one configured YDB database and exposes the virtual
`default` schema. Migrate existing `catalog.ydb.table` references to
`catalog.default.table`. Use separate catalog configurations for other databases.
Directories remain part of the table name:

```sql
SELECT * FROM ydb.default."sales/eu/orders";
```

Paths are relative to the configured database. Absolute paths, empty components,
`.` and `..` are rejected; YDB enforces naming and size limits. JDBC `usePrefixPath`
is unsupported because it changes that root. See [YDB namespace rules](https://ydb.tech/docs/en/concepts/datamodel/cluster-namespace).

Trino lowercases identifiers. The connector preserves the spelling of unique
case-insensitive matches and rejects ambiguity. CREATE/CTAS/RENAME resolve
existing parent directories; the connector neither creates directories nor
exposes them as schemas. Metadata resolution does not lock external renames.

Trino 483 may suppress remote errors in exact-name `information_schema.columns`
lookups; direct table access reports ambiguous names. Optional bulk column
listing remains unsupported.
