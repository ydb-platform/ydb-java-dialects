- Add YDB constraint violation exception mapping to hibernate specific exception 
- Added `'u` suffix to string literals to everywhere used Utf8 type instead of String. For escape symbol added custom translation to it had String type, not Utf8.
- Disable ordinal SELECT item references (`ORDER BY 1`, `GROUP BY 1`) — YDB does not support them

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
