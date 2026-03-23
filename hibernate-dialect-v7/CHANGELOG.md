TODO: use actual version
## 0.9.x ##

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