# Connector review and upstream handoff

This change is delivered to `ydb-platform/ydb-java-dialects`, not to Trino.
The standalone module remains pinned to Trino 483. The upstream comparison
and prototype used Trino `484-SNAPSHOT` at
`8d58343762f56a7d0f13b799f5521ddca6fbcfff`, with Java 25.
Binary or source compatibility between these versions is not assumed.

## Comparison with the pinned Trino tree

| Area | Trino reference | YDB decision |
| --- | --- | --- |
| Packaging | `plugin/trino-postgresql/pom.xml`, root POM, server assembly | An upstream module needs parent-managed `trino-plugin` packaging, root module and server assembly registration, provided SPI dependencies, and generated plugin services. The standalone shaded-driver POM is not an upstream module. |
| SPI | `lib/trino-plugin-toolkit/.../Versions.java` and JDBC sink interfaces | Enforce the compiled SPI version. The 484 provider/sink memory-context signatures differ from 483 and require an explicit port. |
| JOIN | PostgreSQL, MySQL and MariaDB JDBC clients; `DefaultQueryBuilder` | Use framework join plumbing, but a YQL-specific scalar allowlist and key normalization. Do not inherit another database's comparison semantics. |
| Expressions | `ConnectorExpressionRewriter`, JDBC rewrite rules | Preserve parameter occurrence order. Leave overflow-sensitive arithmetic, Unicode trim and unsupported native coercions in Trino. |
| Types | `StandardColumnMappings`, pinned YDB JDBC `MappingGetters`/`YdbTypes` | Use native unsigned and temporal mappings. Uint64 becomes decimal(20,0), not a signed bigint. Decimal writes carry precision and scale. |
| Metadata | `BaseJdbcClient.getColumns` and YDB JDBC metadata | YDB reports neither TABLE_CAT nor TABLE_SCHEM. The virtual `default` schema must not become a metadata row filter or a physical directory. Staging uses the driver's actual identity. |
| Writes | `JdbcPageSink`, `JdbcMergeSink`, `BaseJdbcClient.beginMerge` | Retain ordinary JDBC INSERT plumbing with YDB schema-copy DDL. Row-level writes explicitly require non-transactional mode and use bounded native-typed batches, rather than operation-specific sinks with lost native metadata. |
| Counts | `QueryBuilder.prepareUpdateQuery`/`prepareDeleteQuery` | Pushdown UPDATE/DELETE uses YQL RETURNING, not a preceding COUNT. Row-level counts follow Trino's processed-row contract in the explicitly non-transactional mode. |
| Tests | `BaseConnectorTest`, `BaseConnectorSmokeTest` | Keep inherited capability tests. Add a production-client native JOIN matrix and write tests; the test-only hidden-key client does not establish production CREATE/INSERT semantics. |
| Documentation/CI | `.github/CONTRIBUTING.md`, `.github/DEVELOPMENT.md`, Trino connector docs and CI | An upstream submission also requires documentation registration, formatting, dependency checks, fresh Error Prone, the ordinary CI matrix and CLA. Standalone Maven success does not establish these. |

## Correctness changes

- Equality JOIN covers Bool, signed/unsigned integers, floating point, text,
  bytes and temporal native types, with INNER/LEFT/RIGHT/FULL test cases.
  Uint64 comparisons retain the full range. NaN and signed zero are normalized
  for ordinary equality; null-safe equality, native Decimal and unsafe
  expressions remain in Trino.
- NULLIF and Unicode strpos preserve repeated bind parameters and SQL NULL.
  Integral overflow is not silently converted into YQL NULL or wrapping.
- Native numeric and temporal writers preserve type, range and precision.
  Floating Top-N uses typed NANVL arguments and explicit NULL/NaN ordering.
- INSERT staging copies native columns without confusing Trino's virtual
  schema with JDBC metadata. A separate Serial key avoids reusing the target
  key as staging row identity.
- Physical-key updates fail explicitly before DML. MERGE retains key order,
  handles nullable composite keys and closes each owned batch transaction.
  Failed batches roll back without replay; previous batches can remain.
- Docker unavailability is an integration-test failure, not a silent skip.
  Shared YDB helper use is serialized. CTAS cleanup assertions subscribe to
  the actual rollback completion instead of racing asynchronous cleanup.

## Upstream prototype evidence and limitations

The mistakenly opened `trinodb/trino#31384` is closed. Its CI reported
113 successful checks, 5 failures and 2 cancellations. Failures included
connector tests, fresh Error Prone, commit-message formatting, CLA, and the
aggregate gate. It is not evidence of upstream readiness.

That Linux run did exercise real YDB: the native JOIN class had 25 test
entries, 1 failure and 1 error. The failing cases exposed an overflow assertion
and untyped floating NANVL argument, both addressed in the standalone tests
and implementation. INSERT metadata identity and MERGE failures also required
changes; the old integration result must not be reused as a pass for this tree.

An independent review of prototype commit
`044a7a0646a2271623c854618482f2c94f972dcc` rejected its atomic-MERGE claim:
one writer was not actually enforced, cancellation did not abort the sink,
and staging metadata still used an inconsistent identity.

The separate local Trino checkout preserves follow-up commits:

- `cf6e4cf33f`: abort unfinished MERGE sinks on operator close;
- `bd9a654ce3`: correct INSERT staging metadata identity;
- `dcb449c1f3`: enforce connector limits for unpartitioned MERGE writers.

Its affected core planner/operator group passed 21 tests. Those engine changes
are not part of Trino 483 and are not silently assumed here. The current
standalone implementation deliberately makes only a non-transactional MERGE
claim. Statement-atomic MERGE needs additional design and verification.

The local Trino checkout is
`/Users/kurdyukov-kir/IdeaProjects/ydb-upstream-work/trino`, branch
`add-ydb-connector`. The upstream submission belongs to the student's handoff;
no replacement upstream PR is created by this work.

## Validation

With Temurin 25.0.2, the following standalone command passed 18 tests, with
zero failures, errors or skips, and built the module:

```bash
mvn -B -ntp -f ydb-trino-adapter/pom.xml \
  -Dtest=TestYdbExpressionRewrites,TestYdbColumnMappings,TestYdbJoinMappings,TestYdbMergeSink,TestYdbWriteMetadata \
  verify
```

The local run used a separate Maven repository via `-Dmaven.repo.local`.
Java LSP diagnostics were unavailable because jdtls is not installed.
Docker's Colima socket was unavailable; Docker/Colima was neither restarted
nor repaired. The local full `clean test` attempted 22 entries: 18 passed,
4 integration classes failed in helper setup, with zero errors or skips.
No local integration test completed. Full integration results must come from
the ordinary module CI:

```bash
mvn -B -ntp -f ydb-trino-adapter/pom.xml clean test
```

The current PR's exact-head CI and independent review, not the historical
prototype or earlier PRs, determine whether this branch is ready to merge.
