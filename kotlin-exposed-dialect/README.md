# Kotlin Exposed YDB Dialect

Модуль добавляет поддержку YDB для Kotlin Exposed через JDBC. Диалект описывает YDB-специфичную генерацию SQL и DDL, маппинг типов, работу с `UPSERT`, secondary indexes, TTL, транзакциями и вспомогательными сценариями Exposed DSL/DAO.

Проект предназначен для использования в составе репозитория `ydb-java-dialects` как отдельный Maven-модуль:

```xml
<groupId>tech.ydb.dialects</groupId>
<artifactId>kotlin-exposed-ydb-dialect</artifactId>
<version>0.1.0</version>
```

## Требования

- JDK 17 или новее
- Maven
- Docker и Docker Compose для интеграционных тестов и локального demo
- YDB JDBC Driver
- JetBrains Exposed 1.x

## Подключение к YDB

Для подключения используется `YdbDialectProvider`. Он регистрирует JDBC-драйвер YDB, metadata provider и передаёт Exposed явный dialect.

```kotlin
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import tech.ydb.exposed.dialect.basic.YdbDialectProvider

val db = YdbDialectProvider.connect(
    url = "jdbc:ydb:grpc://localhost:2136/local",
    user = "",
    password = ""
)

transaction(db) {
    // Exposed DSL / DAO code
}
```

По умолчанию используется JDBC driver:

```text
tech.ydb.jdbc.YdbDriver
```

## Пример таблицы

YDB требует, чтобы у каждой таблицы был явно задан `PRIMARY KEY`. Для YDB-специфичных возможностей можно наследоваться от `YdbTable`.

```kotlin
import tech.ydb.exposed.dialect.basic.YdbIndexScope
import tech.ydb.exposed.dialect.basic.YdbIndexSyncMode
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.types.ydbDecimal

object Products : YdbTable("products") {
    val id = integer("id")
    val sku = varchar("sku", 64)
    val name = varchar("name", 255)
    val category = varchar("category", 128)
    val price = ydbDecimal("price", precision = 10, scale = 2)

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, sku)

        secondaryIndex(
            name = "products_category_idx",
            category,
            unique = false,
            scope = YdbIndexScope.GLOBAL,
            syncMode = YdbIndexSyncMode.ASYNC,
            coverColumns = listOf(name, price)
        )
    }
}
```

Такой класс генерирует YDB-compatible `CREATE TABLE` с primary key и secondary index.

## Основные возможности

### SQL и DDL

Реализованы:

- регистрация YDB dialect для Exposed JDBC;
- генерация YDB-compatible `LIMIT` / `OFFSET`;
- генерация `UPSERT`;
- генерация `CREATE TABLE` с обязательным `PRIMARY KEY`;
- создание и удаление secondary indexes;
- поддержка YDB global secondary indexes;
- поддержка `COVER` columns для secondary indexes;
- TTL для таблиц;
- чтение существующих индексов через JDBC metadata.

### UPSERT

YDB имеет собственную команду `UPSERT`. Диалект формирует SQL в формате, который ожидает YDB, включая обязательный список колонок:

```sql
UPSERT INTO products (id, sku, name) VALUES (?, ?, ?)
```

Пример через Exposed DSL:

```kotlin
import org.jetbrains.exposed.v1.jdbc.upsert
import java.math.BigDecimal

Products.upsert {
    it[id] = 1
    it[sku] = "BOOK-001"
    it[name] = "Kotlin in Action"
    it[category] = "books"
    it[price] = BigDecimal("39.90")
}
```

### Типы данных

Поддерживаются стандартные и YDB-специфичные типы:

- `Int16`, `Int32`, `Int64`;
- `Uint64`;
- `Float`, `Double`;
- `Bool`;
- `Utf8`;
- `String` для бинарных данных;
- `Date`;
- `Datetime`;
- `Timestamp`;
- `Interval`;
- `Decimal(p, s)`;
- `Uuid`;
- UUID как `Utf8`;
- UUID как bytes / `String`;
- `Json`.

Для дополнительных типов доступны extension-функции:

```kotlin
ydbDecimal("price", precision = 10, scale = 2)
ydbInterval("duration")
ydbJson("payload")
ydbUuid("id")
ydbUuidUtf8("external_id")
ydbUuidBytes("binary_uuid")
ydbUint64("counter")
```

Для update-expression сценариев с decimal можно использовать literal helper:

```kotlin
import tech.ydb.exposed.dialect.types.ydbDecimalLiteral
import java.math.BigDecimal

it.update(Products.price, ydbDecimalLiteral(BigDecimal("45.00"), 10, 2))
```

## Идентификаторы без AUTO_INCREMENT

YDB не использует SQL `AUTO_INCREMENT` в привычном для реляционных СУБД виде. Диалект явно отклоняет `autoIncrement()` и предоставляет application-side генерацию идентификаторов.

Доступны:

- `YdbGeneratedIds.uuid()`;
- `YdbGeneratedIds.uuidString()`;
- `YdbGeneratedIds.ulid()`;
- `YdbUuidIdTable`;
- `YdbUuidStringIdTable`;
- `YdbUlidTable`;
- `YdbStringIdTable`.

Пример:

```kotlin
import tech.ydb.exposed.dialect.basic.YdbUlidTable

object Events : YdbUlidTable("events") {
    val payload = text("payload")
}
```

## Транзакции и retry

Модуль работает со стандартными Exposed JDBC transactions и добавляет helper для повторного выполнения транзакций при retriable-ошибках YDB.

Поддерживаются режимы:

- `READ_WRITE`;
- `READ_ONLY`.

Retry classifier обрабатывает типичные статусы и сообщения YDB, включая:

- `ABORTED`;
- `UNAVAILABLE`;
- `OVERLOADED`;
- `BAD_SESSION`;
- `SESSION_EXPIRED`;
- `SESSION_BUSY`;
- `TIMEOUT`;
- `UNDETERMINED`.

Повторы выполняются с backoff и jitter.

## Пагинация

Обычная пагинация через `LIMIT` поддерживается на уровне dialect.

Для больших таблиц также доступен keyset pagination helper:

```kotlin
import tech.ydb.exposed.dialect.pagination.keysetPageAsc

val page = Products
    .selectAll()
    .keysetPageAsc(Products.id, lastValue = null, limit = 20)
    .toList()
```

Для обратного порядка используется `keysetPageDesc`.

## Optimistic Locking

Для сценариев с версионированием строк добавлен helper `YdbOptimisticLocking.updateWithVersion`. Он проверяет текущую версию строки и выполняет update только если версия совпадает с ожидаемой.

Типовой сценарий:

- строка содержит колонку `version`;
- клиент передаёт ожидаемую версию;
- helper обновляет строку и увеличивает `version`;
- если версия устарела, update не выполняется.

## Совместимость с Exposed DSL / DAO

Интеграционные тесты покрывают следующие сценарии:

- подключение к YDB;
- CRUD через Exposed DSL;
- `UPSERT`;
- batch operations;
- DAO smoke workflow;
- generated UUID / ULID identifiers;
- secondary indexes;
- TTL;
- JOIN;
- подзапросы;
- many-to-many через связующую таблицу;
- optimistic locking;
- keyset pagination;
- работу с YDB-типами.

## Локальный запуск YDB

В модуле есть `docker-compose.yml` для локальной YDB.

Запуск:

```bash
docker compose up -d
```

YDB будет доступна по адресу:

```text
jdbc:ydb:grpc://localhost:2136/local
```

Web UI:

```text
http://localhost:8765
```

Остановка:

```bash
docker compose down -v
```

## Тесты

Полная проверка:

```bash
mvn clean install
```

Команда запускает:

- unit tests через Maven Surefire;
- integration tests через Maven Failsafe;
- сборку jar;
- установку artifact в локальный Maven repository.

Интеграционные тесты рассчитаны на локальную YDB, поднятую через Docker Compose.

## Demo Application

Demo-приложение находится в package:

```text
tech.ydb.exposed.dialect.demo
```

Оно показывает:

- подключение к YDB;
- создание таблицы;
- генерацию DDL;
- secondary index с `COVER`;
- `UPSERT`;
- чтение данных;
- update decimal-поля;
- keyset pagination;
- delete.

Запуск в PowerShell:

```powershell
mvn --% exec:java -Dexec.mainClass=tech.ydb.exposed.dialect.demo.DemoAppKt
```

Если классы ещё не скомпилированы:

```powershell
mvn --% compile exec:java -Dexec.mainClass=tech.ydb.exposed.dialect.demo.DemoAppKt
```

Запуск в Linux, macOS или cmd:

```bash
mvn exec:java -Dexec.mainClass=tech.ydb.exposed.dialect.demo.DemoAppKt
```

## CI

Для модуля подготовлен GitHub Actions workflow. Он:

- запускается при изменениях в `kotlin-exposed-dialect`;
- поднимает локальную YDB через Docker Compose;
- ожидает доступности YDB;
- выполняет `mvn clean install`;
- выводит логи YDB при ошибке;
- останавливает контейнер после завершения.

## Структура модуля

```text
src/main/kotlin/tech/ydb/exposed/dialect/basic
```

Базовые классы dialect, registration/bootstrap, table helpers, generated IDs, TTL, secondary indexes и metadata.

```text
src/main/kotlin/tech/ydb/exposed/dialect/functions
```

Генерация SQL-конструкций dialect: `UPSERT`, `LIMIT`, обработка `MERGE`.

```text
src/main/kotlin/tech/ydb/exposed/dialect/types
```

YDB data type provider и custom column types.

```text
src/main/kotlin/tech/ydb/exposed/dialect/transaction
```

Retry classifier и transaction helpers.

```text
src/main/kotlin/tech/ydb/exposed/dialect/pagination
```

Keyset pagination helpers.

```text
src/main/kotlin/tech/ydb/exposed/dialect/locking
```

Optimistic locking helper.

```text
src/main/kotlin/tech/ydb/exposed/dialect/demo
```

Консольное demo-приложение.

## Особенности реализации

- Для каждой таблицы требуется явный `PRIMARY KEY`.
- `AUTO_INCREMENT` не используется; вместо него предусмотрены UUID/ULID helpers.
- `UPSERT` реализован через native YDB syntax.
- ANSI `MERGE` не преобразуется в `UPSERT`, поскольку эти операции не являются полными эквивалентами.
- `UNIQUE` secondary indexes и `FOREIGN KEY` не используются как основной механизм моделирования в YDB в рамках данного dialect.
- Metadata layer покрывает сценарии, необходимые для реализованных и протестированных DDL/DSL workflow.
