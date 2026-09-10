package ydb.jimmer.dialect;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NativeBuilder;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class YdbKeysetPaginator {
    private final JSqlClient sqlClient;

    public YdbKeysetPaginator(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    public static class Page<R> {
        private final List<R> rows;
        private final List<Object> nextCursor;

        public Page(List<R> rows, List<Object> nextCursor) {
            this.rows = rows;
            this.nextCursor = nextCursor;
        }

        public List<R> getRows() {
            return rows;
        }

        public List<Object> getNextCursor() {
            return nextCursor;
        }

        public boolean hasNextPage() {
            return nextCursor != null;
        }
    }

    public <T extends TableProxy<E>, E, R> Page<R> fetchPage(
            T table,
            List<Expression<?>> keyColumns,
            List<Object> cursor,
            int limit,
            Function<R, List<Object>> keyExtractor,
            BiFunction<MutableRootQuery<T>, T, ConfigurableRootQuery<T, R>> querySetup
    ) {
        if (keyColumns.isEmpty()) {
            throw new IllegalArgumentException("keyColumns must not be empty");
        } else if (cursor != null && cursor.size() != keyColumns.size()) {
            throw new IllegalArgumentException(
                    "cursor has " + cursor.size() + " values, but keyColumns has " + keyColumns.size()
            );
        }

        MutableRootQuery<T> query = sqlClient.createQuery(table);

        if (cursor != null) {
            query.where(buildKeysetPredicate(keyColumns, cursor));
        }

        ConfigurableRootQuery<T, R> configured = querySetup.apply(query, table);

        List<R> fetched = configured.limit(limit + 1, 0L).execute();

        boolean hasMore = fetched.size() > limit;
        List<R> pageRows = hasMore ? new ArrayList<>(fetched.subList(0, limit)) : fetched;

        List<Object> nextCursor = hasMore
                ? keyExtractor.apply(pageRows.get(pageRows.size() - 1))
                : null;

        return new Page<>(pageRows, nextCursor);
    }

    private static Predicate buildKeysetPredicate(
            List<Expression<?>> keyColumns,
            List<Object> cursorValues
    ) {
        int n = keyColumns.size();

        StringBuilder sql = new StringBuilder("(");

        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sql.append(", ");
            }

            sql.append("%e");
        }

        sql.append(") > (");

        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sql.append(", ");
            }

            sql.append("%v");
        }

        sql.append(")");

        NativeBuilder.Prd builder = Predicate.sqlBuilder(sql.toString());
        for (Expression<?> col : keyColumns) {
            builder.expression(col);
        }

        for (Object val : cursorValues) {
            builder.value(val);
        }

        return builder.build();
    }
}
