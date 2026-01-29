## 1.3.1 ##

- Fixed bug: Allow setting fields in UpdateOperation without aliasing names ([pr](https://github.com/ydb-platform/ydb-java-dialects/pull/201)).

## 1.3.0 ##

- Added support for using view primary keys in `HintedTable`.

## 1.2.1 ##

- Fixed bug: Handling null values in the Converter ([#190](https://github.com/ydb-platform/ydb-java-dialects/pull/190))

## 1.2.0

- Support for `Date32`, `Datetime64`, and `Timestamp64` YDB types.

## 1.1.1 ##

- Supported UUID YDB Type

## 1.1.0 ##
- Upgrade to new version of JDBC & SDK

## 1.0.1 ##
- Fixed bug with `useIndex` - skip the table name (`from view`)

## 1.0.0 ##

- `REPLACE` / `UPSERT` builders from YDB
- Supported VIEW INDEX from `useIndex("index_name")` HintedTable
- Generated tables from schema
