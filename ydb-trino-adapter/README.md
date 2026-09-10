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

## Каталог и схема

Имя каталога задаёт файл конфигурации Trino. В примере
[`local.properties`](examples/trino/etc/catalog/local.properties) каталог `local`
подключён к базе YDB `/local`. Для другой базы создайте отдельный файл каталога
с её JDBC URL. Внутри каталога адаптер показывает схему `default`:

```sql
SELECT * FROM local.default.orders;
```

Прежние обращения `catalog.ydb.table` нужно заменить на `catalog.default.table`.

## Текст и байты

YDB `Text` отображается в Trino как `varchar`, а `Bytes` — как `varbinary` без
декодирования UTF-8. При создании таблиц адаптер использует типы `Text` и `Bytes`.

Trino `date` создаёт YDB `Date32`; существующие колонки YDB `Date` также читаются
как `date` и не мигрируются. Значения ограничены диапазоном `Date32`.
