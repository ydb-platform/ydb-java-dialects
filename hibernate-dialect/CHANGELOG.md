## 1.5.0 ##

- Support for `GenerationType.IDENTITY` with JDBC YDB Driver **2.3.11** and later.
- Support for `Date32`, `Datetime64`, and `Timestamp64` YDB types.

## 1.4.1 ##

- Deleted `InExpressionCountLimit`

## 1.4.0 ##

- Added hint for YQL pragma queries

## 1.3.0 ##

- Added support UUID YDB type

## 1.2.0 ##

- Added custom decimal jdbc codes `DECIMAL_31_9`, `DECIMAL_35_0`, `DECIMAL_35_9`

## 1.1.0 ##

- Added hint for scan queries

## 1.0.0 ##

- Fixed: data time type converters

## 0.9.5 ##

- Added query hint for view index for "select * from ... where" queries

## 0.9.4 ##

- Fixed the generated bool value

## 0.9.3 ##

- Supported enum field of Entity

## 0.9.2 ##

- Supported LocalDateTime with datetime YDB primitive type and mapped to java.sql.Types.TIME

## 0.9.1 ##

- Full CRUD operations
- Support LIKE / ILIKE statement with ESCAPE
- Support LIMIT ? OFFSET ? statement
- Support ORDER BY ? statement
- @OneToOne, @ManyToMany, @OneToMany, @ManyToOne
- Generate table schema