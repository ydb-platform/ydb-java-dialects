# Kotlin Exposed YDB Dialect

Проект реализует SQL-диалект YDB для фреймворка Kotlin Exposed и позволяет использовать Exposed DSL/DAO для работы с YDB через JDBC.

## Поддержка YDB в Kotlin Exposed (YDB SQL Dialect + JDBC интеграция)

### Описание предметной области
Exposed — ORM/SQL‑DSL для Kotlin. Требует диалект БД для генерации SQL и DDL через JDBC. YDB — распределённая транзакционная СУБД (аналог «SQL поверх key‑value») с собственным SQL/YQL, UPSERT, глобальными вторичными индексами и транзакциями с ретраями.   
Цель проекта — добавить полноценный диалект YDB в Exposed, чтобы писать к YDB обычный Kotlin‑DSL/DAO без ручного SQL.

### Что следует сделать:

Реализовать диалект Exposed для YDB (JDBC): синтаксис идентификаторов, LIMIT, UPSERT/MERGE, генерация DDL (CREATE TABLE с обязательным PK), индексы (GSI), TTL.  
Маппинг типов YDB ↔ Exposed: числовые, строка/Bytes, Bool, Date/Datetime/Timestamp/Interval, Decimal(p,s), UUID (String/Bytes), JSON.  
Транзакции и ретраи: обработка abort/timeout с backoff, read-only/read-write, батчи, пагинация (LIMIT и keyset-хелпер).  
Совместимость DSL/DAO: JOIN, подзапросы, many-to-many, optimistic locking (version column); при необходимости эмуляция UNIQUE/FOREIGN KEY, генерация идентификаторов без AUTO_INCREMENT (UUID/ULID).  
Тесты и примеры: юнит и интеграционные (YDB в Docker), CI, демо-приложение (CRUD/индексы/пагинация), документация с ограничениями и рецептами.

## Статус

Проект находится в рабочем состоянии и покрыт unit- и integration-тестами.  
Реализован базовый функциональный слой диалекта, типы YDB, DDL/DDL-расширения, CRUD-сценарии, запросы на нескольких таблицах, batch-операции, keyset pagination, optimistic locking, demo-приложение и CI.

## Поддерживаемые возможности

### SQL / DDL

Поддерживаются:

- генерация идентификаторов и SQL в стиле YDB;
- `LIMIT`;
- `UPSERT`;
- `CREATE TABLE` с обязательным `PRIMARY KEY`;
- secondary indexes для YDB;
- TTL для таблиц YDB;
- `ALTER TABLE ... ADD INDEX` в рамках поддерживаемой модели secondary index.

### Маппинг типов YDB ↔ Exposed

Поддерживаются:

- целочисленные типы: `Int16`, `Int32`, `Int64`;
- беззнаковые типы, используемые в YDB-расширениях диалекта, включая `Uint64`;
- строковые типы: `Utf8`;
- бинарные данные: `String`/bytes-представление;
- `Bool`;
- `Float`, `Double`;
- `Date`, `Datetime`, `Timestamp`;
- `Interval`;
- `Decimal(p, s)`;
- `UUID`:
    - native `Uuid`,
    - UUID как `Utf8`,
    - UUID как bytes/`String`;
- `JSON`.

### Транзакции и выполнение запросов

Поддерживаются:

- обычные транзакции Exposed поверх YDB JDBC;
- retry-классификация для retriable-сценариев;
- batch operations;
- обычная пагинация через `LIMIT`;
- keyset pagination helper.

### Совместимость с Exposed DSL / DAO

Поддерживаются и протестированы:

- CRUD через Exposed DSL;
- `JOIN`;
- подзапросы;
- many-to-many через связующую таблицу;
- DAO basic workflow;
- optimistic locking через version column pattern.

### Тесты и примеры

В проекте есть:

- unit tests для отдельных частей диалекта;
- integration tests с локальной YDB в Docker;
- GitHub Actions CI;
- консольное demo-приложение.

## Ограничения текущей реализации

На текущем этапе важно учитывать следующие ограничения:

- YDB требует явный `PRIMARY KEY` для каждой таблицы.
- `AUTO_INCREMENT` в привычном SQL-смысле не используется; идентификаторы должны задаваться явно или генерироваться на уровне приложения.
- Secondary indexes в проекте ориентированы на текущую модель YDB global secondary indexes.
- `UNIQUE` secondary indexes не считаются поддержанными в текущем tested runtime и не должны использоваться как рабочий путь.
- Metadata/introspection layer (`YdbDialectMetadata`) реализован минимально; основной акцент сделан на генерации и выполнении SQL/DDL, а не на полном schema inspection.
- `FOREIGN KEY` и полноценные SQL-ограничения уникальности не являются основным механизмом моделирования в YDB и не рассматриваются как полностью поддержанный слой диалекта.
- Для `Decimal(p, s)` рекомендуется использовать YDB-специфичные расширения диалекта:
    - `ydbDecimal(...)` для колонки,
    - `ydbDecimalLiteral(...)` для update-expression сценариев, где требуется корректный decimal literal в YDB.

## Структура проекта

Основные части проекта:

- диалект YDB для Exposed;
- провайдеры SQL/DDL и типов;
- YDB-специфичные column types и helpers;
- unit tests;
- integration tests;
- demo-приложение;
- CI-конфигурация.

## Как поднять YDB локально

Для запуска integration tests и demo-приложения требуется локальный экземпляр YDB, доступный по адресу:

```text
grpc://localhost:2136/local
```

В корне проекта выполните:

```bash
docker compose up -d
```

После запуска контейнеру требуется несколько секунд на инициализацию. До завершения инициализации база может быть недоступна.

Остановить локальный YDB можно командой:
```bash
docker compose down
```

Локальный web UI YDB доступен по адресу:
```text
http://localhost:8765
```

### Как запустить тесты

После запуска локального YDB выполните:
```bash
mvn clean install
```
Эта команда запускает unit tests, integration tests и сборку артефакта.

## Демо-приложение

В проекте есть консольное демо-приложение, показывающее работу диалекта на реальной локальной YDB:

### Что показывает demo

Demo демонстрирует:

создание таблицы demo_products;
создание secondary index по полю category;
CRUD-операции:
вставка тестовых данных;
чтение данных по категории;
обновление записи;
удаление записи;
keyset pagination по первичному ключу;
вывод сгенерированного DDL и результатов выполнения операций в консоль.

## Как запустить демо:

Сначала поднимите локальный YDB:
```bash
docker compose up -d
```

Далее запустите demo

**Linux / macOS / cmd**
```bash
mvn compile exec:java -Dexec.mainClass="tech.ydb.exposed.dialect.demo.DemoAppKt"
```

**PowerShell**
```bash
mvn --% compile exec:java -Dexec.mainClass=tech.ydb.exposed.dialect.demo.DemoAppKt
```

## Что проверяют тесты

Тестовый набор покрывает основные сценарии использования диалекта:

- подключение к локальной YDB;
- DDL и создание таблиц;
- CRUD;
- UPSERT;
- batch operations;
- secondary indexes;
- TTL;
- типы данных;
- JOIN, подзапросы, many-to-many;
- optimistic locking;
- keyset pagination;
- DAO basic workflow;
- предметный integration scenario на нескольких таблицах.

Это подтверждает корректную работу диалекта в рамках реализованного функционального подмножества.

## CI

В проекте настроен GitHub Actions workflow, который поднимает локальный YDB в CI и запускает сборку и тесты проекта.

## Практические рекомендации

При использовании диалекта рекомендуется:

всегда задавать явный PRIMARY KEY;
использовать YDB-специфичные типы и helpers там, где они уже предусмотрены проектом;
для Decimal(p, s) использовать ydbDecimal(...);
для decimal update-expression использовать ydbDecimalLiteral(...);
не рассчитывать на AUTO_INCREMENT;
воспринимать UNIQUE secondary index как неподдержанный сценарий текущего tested runtime.
