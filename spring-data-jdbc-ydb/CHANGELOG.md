## 1.2.2 ##

- Fixed YDB Decimal

## 1.2.1 ##

- Reverted public visible for YdbSqlType

## 1.2.0 ##

- Support for `Date32`, `Datetime64`, and `Timestamp64` YDB types.

## 1.1.1 ##

- Support `UUID` YDB type.

## 1.1.0 ##

- Fixed bug: No MethodInvocation found for Spring version 3.4.0
- Added JdbcRepositoryBeanPostProcessor for @ViewIndex annotation

## 1.0.0 ##

- quoting index name in VIEW statement (ex. VIEW \`index_name\`)

## 0.9.1 ##

- Supported VIEW statement from @ViewIndex
- YdbDialect fully supports YQL
- Supported specific @YdbType