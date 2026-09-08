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

## JOIN pushdown

JOIN pushdown включается отдельно для каталога:

```properties
join-pushdown.enabled=true
```

Или для сессии Trino:

```sql
SET SESSION local.join_pushdown_enabled = true;
```

Адаптер передаёт в YDB `INNER`, `LEFT`, `RIGHT` и `FULL JOIN`, когда условия
состоят из равенств физических столбцов `Int64` с разных сторон, соединённых
через `AND`. Поддерживаются nullable-ключи, несколько ключей и вложенные JOIN.
Условия с другими типами, выражениями, неравенствами, `IS NOT DISTINCT FROM`
или только одной стороной остаются в Trino. JOIN между каталогами также
выполняется в Trino. Ограничения `ON` описаны в
[документации YQL](https://ydb.tech/docs/en/yql/reference/syntax/select/join).

Используется структурированный JOIN API Trino 483. Его устаревший переключатель
`deprecated.join-pushdown.with-expressions` по умолчанию выключен в адаптере;
при явном включении этого переключателя JOIN остаётся в Trino. Стоимость JOIN
не оценивается адаптером: включение pushdown может увеличить объём данных,
возвращаемых из YDB.
