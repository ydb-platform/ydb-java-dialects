package ydb.jimmer.dialect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.TransactionCacheOperator;
import org.babyfish.jimmer.sql.cache.UsedCache;
import org.babyfish.jimmer.sql.exception.ExecutionException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Uses UUID for the ids in the {@link #TABLE_NAME} SQL table.
 */
public class UuidTransactionCacheOperator extends TransactionCacheOperator {
    public static final String TABLE_NAME = "JIMMER_TRANS_CACHE_OPERATOR";

    private static final String ID = "ID";

    private static final String IMMUTABLE_TYPE = "IMMUTABLE_TYPE";

    private static final String IMMUTABLE_PROP = "IMMUTABLE_PROP";

    private static final String CACHE_KEY = "CACHE_KEY";

    private static final String REASON = "REASON";

    private static final String INSERT_WITH_UUID =
            "insert into " +
                    TABLE_NAME + "(" +
                    ID +
                    ", " +
                    IMMUTABLE_TYPE +
                    ", " +
                    IMMUTABLE_PROP +
                    ", " +
                    CACHE_KEY +
                    ", " +
                    REASON +
                    ") values(?, ?, ?, ?, ?)";

    private final ObjectMapper mapper;

    private final int batchSize;

    public UuidTransactionCacheOperator() {
        this(null, 32);
    }

    public UuidTransactionCacheOperator(int batchSize) {
        this(null, batchSize);
    }

    public UuidTransactionCacheOperator(ObjectMapper mapper) {
        this(mapper, 32);
    }

    public UuidTransactionCacheOperator(ObjectMapper mapper, int batchSize) {
        super(mapper, batchSize);

        if (batchSize < 1) {
            throw new IllegalArgumentException("`batchSize` cannot be less than 1");
        }
        this.mapper = mapper != null ?
                mapper :
                new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .registerModule(new ImmutableModule());
        this.batchSize = batchSize;
    }

    @Override
    public void delete(UsedCache<Object, ?> cache, Object key, Object reason) {
        if (reason != null && !(reason instanceof String)) {
            throw new IllegalArgumentException(
                    "The cache deletion reason can only be null or string when trigger type is `TRANSACTION_ONLY`"
            );
        }
        save(cache.type(), cache.prop(), Collections.singleton(key), (String) reason);
    }

    @Override
    public void deleteAll(UsedCache<Object, ?> cache, Collection<Object> keys, Object reason) {
        if (keys.isEmpty()) {
            return;
        }
        if (reason != null && !(reason instanceof String)) {
            throw new IllegalArgumentException(
                    "The cache deletion reason can only be null or string when trigger type is `TRANSACTION_ONLY`"
            );
        }
        save(cache.type(), cache.prop(), keys, (String) reason);
    }

    private void save(
            ImmutableType type,
            ImmutableProp prop,
            Collection<Object> keys,
            String reason
    ) {
        sqlClient().getConnectionManager().execute(con -> {
            try {
                try (PreparedStatement stmt = con.prepareStatement(INSERT_WITH_UUID)) {
                    int count = 0;
                    for (Object key : keys) {
                        stmt.setString(1, UUID.randomUUID().toString());
                        stmt.setString(2, type != null ? type.toString() : null);
                        stmt.setString(3, prop != null ? prop.toString() : null);
                        stmt.setString(4, mapper.writeValueAsString(key));
                        stmt.setString(5, reason);
                        stmt.addBatch();

                        if (++count % batchSize == 0) {
                            stmt.executeBatch();
                        }
                    }

                    if (count % batchSize != 0) {
                        stmt.executeBatch();
                    }
                }
            } catch (SQLException | JsonProcessingException ex) {
                throw new ExecutionException("Failed to save delayed cache deletion", ex);
            }
            return null;
        });
    }
}
