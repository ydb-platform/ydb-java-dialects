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