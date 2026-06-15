## 0.10.0 ##

- Renamed the `maxRetries` retry setting to `maxAttempts`, which now counts the total number of attempts including the initial execution (aligned with Spring Retry semantics). The `ydb.transaction.retry.max-retries` property is renamed to `ydb.transaction.retry.max-attempts`.
- `@YdbTransactional` override attributes now use `0` (instead of `-1`) to inherit the global configuration value; negative values are rejected.

## 0.9.0 ##

- First version of the plugin
