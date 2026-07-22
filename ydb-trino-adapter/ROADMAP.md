# YDB Trino Adapter — roadmap

Статус относительно Trino `BaseConnectorTest` / `BaseConnectorSmokeTest`
(ветка `ydb-trino-17-07`, ~314 тестов: ~187 green, ~127 skipped).

Skipped почти всегда означают `hasBehavior(...)=false` или явный `NOT_SUPPORTED`
в `YdbClient`, а не случайный флаaky. Пустые `@Override` (тест «проходит», но ничего
не проверяет) отмечены отдельно — это тоже долг.

## Уже есть

- CREATE / DROP TABLE, INSERT (non-transactional), SELECT
- Predicate pushdown, в том числе по `varchar` (`FULL_PUSHDOWN`, bind через JDBC → `Text`/`Utf8`)
- LIMIT / TopN pushdown (включая TopN по varchar)
- Базовый набор скалярных типов: bool, int*, float/double, decimal, date, timestamp, varchar/Text
- Агрегации и часть expression rewrite (arithmetic, `IN`, string ops, …)

## Phase 0 — честный тест-долг (быстро)

Сейчас часть тестов green из‑за пустого override. Либо починить, либо явно
документировать/оставить skip с причиной.

| Тест / тема | Проблема |
|---|---|
| Long table / column names | Лимиты имён YDB |
| Negative dates / year-of-era | Нет отрицательных дат в YQL |
| `testCharVarcharComparison` | CHAR без pad → не семантика Trino CHAR |
| `testVarcharCastToDateInPredicate` | Cast/pushdown не поддержан |
| Row-level UPDATE declaration / `testRowLevelUpdate` | Planner падает до `NOT_SUPPORTED` |
| `testInsertForDefaultColumn` | Нет default columns |

**Критерий готовности:** нет «пустых» overrides без комментария «unsupported by design».

## Phase 1 — DDL, которое уже есть в YQL (высокий ROI)

YDB умеет `ALTER TABLE ... ADD/DROP COLUMN`, `SET/DROP NOT NULL`. В адаптере это
сейчас выключено.

1. **DROP COLUMN** — снять `SUPPORTS_DROP_COLUMN=false`, реализовать в `YdbClient`
2. **ADD COLUMN** (без comment / position) — базовая добавка nullable-колонок
3. **DROP / SET NOT NULL** — если поведение совпадёт с ожиданиями Trino-тестов

Ожидаемый эффект: разблокировка пачки `testDrop*Column`, `testAddAndDropColumnName`,
части not-null тестов (~10–20 кейсов).

**Вне scope phase 1:** rename column, `SET DATA TYPE`, column/table comments,
`ADD COLUMN ... NOT NULL` с backfill-семантикой Trino, `WITH POSITION`.

## Phase 2 — UPDATE / DELETE (без MERGE)

В YQL есть `UPDATE` / `DELETE`. Коннектор сейчас бросает `MODIFYING_ROWS_MESSAGE`.

1. Простой `DELETE` / `UPDATE` с pushdown предикатов (в т.ч. varchar через `?`)
2. Сложные предикаты из BaseConnectorTest (LIKE, subquery, semi-join) — по мере готовности
3. **MERGE не целиться** — в YDB нет Trino-MERGE; оставляем `SUPPORTS_MERGE=false`
   (или позже эмулировать через UPSERT, отдельным решением)

Ожидаемый эффект: до ~30–40 тестов из группы delete/update (без merge-сюиты).

**Риски:** семантика транзакций, `testRollback` / `testInsertInTransaction`,
row-level update planner quirks, written stats.

## Phase 3 — schema as path (дизайн)

Trino `CREATE SCHEMA` ≠ SQL schema в YDB. Схемы естественно мапятся на директории
в path БД.

Варианты:

- оставить один schema `ydb` (как сейчас) — просто и предсказуемо;
- мапить `schema` → subdirectory + реализовать create/drop/rename directory.

Без явного дизайн-решения флаги `SUPPORTS_CREATE_SCHEMA` / rename / cascade
не включать. Cascade + views/MV — отдельно.

## Phase 4 — типы контейнеров

| Trino | YDB | Статус |
|---|---|---|
| `ARRAY` | `List` | не замаплено → skip insert/array/field-in-array |
| `MAP` | `Dict` | не замаплено |
| `ROW` | `Struct` | не замаплено → skip row-field + projection pushdown по nested |

Нужны read/write mappings, predicate/projection pushdown, тесты data-mapping.
Крупный объём, лучше отдельными PR по типу.

## Phase 5 — Views / comments / прочее

- **Views:** в YDB есть ограниченная поддержка; Trino VIEW + metadata-тесты —
  отдельный трек. Materialized / federated MV — низкий приоритет.
- **COMMENT ON TABLE/COLUMN:** семантика Trino не совпадает 1:1 с YDB table/column
  properties — не блокирует core DML/DDL.
- **RENAME COLUMN / SET COLUMN TYPE:** проверить актуальные возможности YQL;
  сейчас считаем unsupported.
- **RENAME TABLE across schemas:** зависит от phase 3 (path move).

## Порядок PR (предложение)

```text
0. ROADMAP + подчистить пустые overrides (docs/honesty)
1. DROP COLUMN (+ минимальные ADD COLUMN)
2. UPDATE / DELETE (простые предикаты) → расширять предикаты
3. Design note: schema-as-path (да/нет) → реализация или явный отказ
4. List / Dict / Struct mappings по одному типу
5. Views / comments по необходимости продукта
```

## Как мерить прогресс

После каждого PR:

```bash
cd ydb-trino-adapter
mvn test
# смотреть surefire: Tests run / Skipped / Failures
```

Целевые ориентиры (грубо):

| Milestone | Skipped (ориентир) |
|---|---|
| Сейчас | ~127 |
| После phase 1 | ~110 |
| После phase 2 | ~70–80 |
| После phase 4 | заметно ниже за счёт array/map/row |

Точные числа зависят от того, сколько тестов завязано на комбинации флагов
(например MERGE останется большим блоком skip).

## Не делать

- Включать `hasBehavior=true` без реализации в `YdbClient` / QueryBuilder
- Ослаблять CI workflow, чтобы «позеленеть»
- Эмулировать MERGE «лишь бы тесты» без явной семантики и документации
