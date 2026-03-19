TODO: Set actual version
## 0.9.x ##

- Fixed `lower` and `upper` functions: replaced `Unicode::ToLower`/`Unicode::ToUpper` with YQL built-in functions compatible with `Text`/`String` column types

## 0.9.2 ##

- Support `concat` function

## 0.9.1 ##

- Full CRUD operations
- Support LIKE / ILIKE statement with ESCAPE
- Support LIMIT ? OFFSET ? statement
- Support ORDER BY ? statement
- @OneToOne, @ManyToMany, @OneToMany, @ManyToOne
- Generate table schema
- Support `lower` and `upper` functions