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

## Создание таблиц

YDB требует первичный ключ, поэтому `CREATE TABLE` и `CREATE TABLE AS` должны
задавать упорядоченное свойство `primary_key`. Адаптер не добавляет скрытый ключ:

```sql
CREATE TABLE events (tenant bigint, event_id bigint, payload varchar)
WITH (primary_key = ARRAY['tenant', 'event_id']);

CREATE TABLE events_copy
WITH (primary_key = ARRAY['tenant', 'event_id'])
AS SELECT tenant, event_id, payload FROM events;
```

Имена ключей в CTAS относятся к выходным столбцам запроса. Отсутствующий,
пустой, повторяющийся или неизвестный `primary_key` отклоняется. Текущая
integration-проверка CTAS использует `insert.non-transactional-insert.enabled=true`;
transactional staging этой проверкой не подтверждается.

## JOIN pushdown

По умолчанию JOIN выполняет Trino. Для пробного pushdown задайте
`join_pushdown_enabled=true` в сессии каталога. Адаптер передаёт YDB
`INNER`, `LEFT`, `RIGHT` и `FULL JOIN` только по равенству исходных столбцов
с совместимыми отображениями: `Bool`, знаковые и беззнаковые целые,
`Float`/`Double`, текст, байты, даты и timestamps. Поддерживается составной ключ.
`NULL` и `NaN` не совпадают по обычному равенству; `-0.0` совпадает с `0.0`.
Для YQL ключи с `NaN` заменяются на `NULL`, а нули нормализуются.
Native `Decimal`, null-safe equality, неравенства, вычисляемые ключи и
неподдерживаемые преобразования остаются в Trino.
См. [правила YQL JOIN](https://ydb.tech/docs/ru/yql/reference/syntax/select/join).

## Числовые типы

`Uint8`, `Uint16`, `Uint32` отображаются соответственно в `smallint`,
`integer`, `bigint`. `Uint64` отображается в `decimal(20,0)` и сохраняет
весь диапазон до `18446744073709551615`, без превращения старшего бита в знак.
Запись проверяет границы unsigned-типа. `Decimal` записывается с точными
precision и scale, включая `NULL`; максимальная поддерживаемая precision — 35.
Native decimal `NaN` и бесконечности не представимы в Trino `decimal`.

Целочисленные и decimal `sum`/`avg`, а также группировки и `min`/`max`
для floating-point выполняет Trino там, где YQL отличается по переполнению,
порядку `NaN` или знаковому нулю. Top-N явно учитывает SQL `NULL` и порядок
`NaN`, используя типизированные floating-point значения в YQL.

## Текст и байты

Вычисления целочисленного сложения, вычитания, умножения и отрицания остаются
в Trino: переполнение в YQL не обязано приводить к той же ошибке. Деление
передаётся в YDB только для целочисленного результата с ненулевым постоянным
делителем, отличным от `-1`. `strpos` считает позиции Unicode-символов и
сохраняет SQL `NULL`, а не заменяет его нулём.

YDB `Text` отображается в Trino как `varchar`, а `Bytes` — как `varbinary` без
декодирования UTF-8. При создании таблиц адаптер использует типы `Text` и `Bytes`.

## Даты

Новые столбцы Trino `date` создаются как YDB `Date32`.
Существующие столбцы YDB `Date` и `Date32` читаются как Trino `date`.
Новые столбцы Trino `timestamp(3)` и `timestamp(6)` создаются как YDB `Timestamp64`.
Существующие YDB `Datetime`, `Datetime64`, `Timestamp` и `Timestamp64` читаются как Trino `timestamp(6)`;
исходная точность `timestamp(3)` в метаданных не сохраняется.
Чтение timestamps использует UTC, а не часовой пояс JVM. Запись дробных секунд
в `Datetime`/`Datetime64` отклоняется без молчаливого усечения.
При записи адаптер передаёт YDB JDBC точный vendor type по `TYPE_NAME` и больше не задаёт `forceSignedDatetimes`:
`Date`/`Date32` получают дни от эпохи, `Datetime`/`Datetime64` — секунды UTC, а `Timestamp`/`Timestamp64` — `Instant` с микросекундами.
В `UPDATE`/`DELETE`, которые стандартный merge sink выполняет внутри `MERGE`, native `TYPE_NAME` не передаётся;
fallback `Instant` для `Datetime`/`Datetime64` может зависеть от часового пояса JVM и остаётся отдельным риском.
Предикаты для `Date`/`Date32` и legacy temporal типов оставляются в Trino,
чтобы не связывать значения вне диапазона исходного YDB-типа.
