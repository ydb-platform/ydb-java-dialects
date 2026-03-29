## Поддержка YDB в Kotlin Exposed (YDB SQL Dialect + JDBC интеграция)

### Описание предметной области
Exposed — ORM/SQL‑DSL для Kotlin. Требует диалект БД для генерации SQL и DDL через JDBC. YDB — распределённая транзакционная СУБД (аналог «SQL поверх key‑value») с собственным SQL/YQL, UPSERT, глобальными вторичными индексами и транзакциями с ретраями. Задача — добавить полноценный диалект YDB в Exposed, чтобы писать к YDB обычный Kotlin‑DSL/DAO без ручного SQL.

### Что следует сделать:

Реализовать диалект Exposed для YDB (JDBC): синтаксис идентификаторов, LIMIT, UPSERT/MERGE, генерация DDL (CREATE TABLE с обязательным PK), индексы (GSI), TTL.  
Маппинг типов YDB ↔ Exposed: числовые, строка/Bytes, Bool, Date/Datetime/Timestamp/Interval, Decimal(p,s), UUID (String/Bytes), JSON.  
Транзакции и ретраи: обработка abort/timeout с backoff, read-only/read-write, батчи, пагинация (LIMIT и keyset-хелпер).  
Совместимость DSL/DAO: JOIN, подзапросы, many-to-many, optimistic locking (version column); при необходимости эмуляция UNIQUE/FOREIGN KEY, генерация идентификаторов без AUTO_INCREMENT (UUID/ULID).  
Тесты и примеры: юнит и интеграционные (YDB в Docker), CI, демо-приложение (CRUD/индексы/пагинация), документация с ограничениями и рецептами.
