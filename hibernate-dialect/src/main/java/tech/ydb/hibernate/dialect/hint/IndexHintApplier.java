package tech.ydb.hibernate.dialect.hint;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class IndexHintApplier {
    private final char[] sql;
    private final Map<String, List<IndexTypedHint>> typedHints;
    private final String shortIndex;
    private final StringBuilder out;

    private int pos;
    private int emitted;
    private int parenLevel;
    private boolean changed;

    private IndexHintApplier(String query, Map<String, List<IndexTypedHint>> typedHints, String shortIndex) {
        this.sql = query.toCharArray();
        this.typedHints = typedHints;
        this.shortIndex = shortIndex;
        this.out = new StringBuilder();
    }

    static String apply(String query, Map<String, List<IndexTypedHint>> typedHints, String shortIndex) {
        return new IndexHintApplier(query, typedHints, shortIndex).apply();
    }

    private String apply() {
        while (pos < sql.length) {
            if (parenLevel != 0 || !isIdentifierStart(sql[pos])) {
                consumeChar();
                continue;
            }

            String keyword = readIdentifier();
            if (!isClauseKeyword(keyword)) {
                continue;
            }

            if (keyword.equalsIgnoreCase("from")) {
                if (shortIndex != null) {
                    TableRef fromTable = parseTableRef();
                    if (fromTable != null) {
                        emitView(fromTable, shortIndex);
                    }
                }
            } else if (keyword.equalsIgnoreCase("join")) {
                TableRef joinTable = parseTableRef();
                if (joinTable != null && joinTable.alias != null) {
                    String index = bestIndex(joinTable.table, collectAliasColumnsUntilNextClause(joinTable.alias));
                    if (index != null) {
                        emitView(joinTable, index);
                    }
                }
            }
        }

        if (!changed) {
            return new String(sql);
        }
        out.append(sql, emitted, sql.length - emitted);
        return out.toString();
    }

    private void emitView(TableRef table, String indexes) {
        out.append(sql, emitted, table.afterTable - emitted);
        out.append(" view ").append(indexes).append(' ');
        emitted = table.aliasStart;
        changed = true;
    }

    private TableRef parseTableRef() {
        skipWhitespace();
        String table = readIdentifier();
        if (table == null) {
            return null;
        }

        int afterTable = pos;
        skipWhitespace();
        String next = peekIdentifier();
        if (next != null && next.equalsIgnoreCase("view")) {
            return null;
        }
        if (next == null || isClauseKeyword(next) || next.equalsIgnoreCase("on")) {
            return new TableRef(table, null, afterTable, pos);
        }

        int aliasStart = pos;
        String alias = readIdentifier();
        if (alias != null && alias.equalsIgnoreCase("as")) {
            skipWhitespace();
            alias = readIdentifier();
        }

        return new TableRef(table, alias, afterTable, aliasStart);
    }

    private Set<String> collectAliasColumnsUntilNextClause(String alias) {
        Set<String> columns = new HashSet<>();
        while (pos < sql.length) {
            if (parenLevel == 0 && startsClauseKeyword()) {
                break;
            }
            if (readAliasColumnInto(alias, columns)) {
                continue;
            }
            consumeChar();
        }
        return columns;
    }

    private boolean readAliasColumnInto(String alias, Set<String> columns) {
        if (pos >= sql.length || !isIdentifierStart(sql[pos])) {
            return false;
        }

        int saved = pos;
        String ident = readIdentifier();
        if (ident == null) {
            pos = saved;
            return false;
        }
        if (parenLevel == 0 && isClauseKeyword(ident)) {
            pos = saved;
            return false;
        }

        if (ident.equals(alias) && pos < sql.length && sql[pos] == '.') {
            pos++;
            skipWhitespace();
            String column = readIdentifier();
            if (column != null) {
                columns.add(column);
            }
        }
        return true;
    }

    private boolean startsClauseKeyword() {
        if (pos >= sql.length || !isIdentifierStart(sql[pos])) {
            return false;
        }
        int saved = pos;
        boolean clause = isClauseKeyword(readIdentifier());
        pos = saved;
        return clause;
    }

    private String bestIndex(String table, Set<String> referencedColumns) {
        List<IndexTypedHint> hints = typedHints.get(table);
        if (hints == null) {
            return null;
        }
        IndexTypedHint best = null;
        for (IndexTypedHint hint : hints) {
            if (!referencedColumns.containsAll(hint.columns)) {
                continue;
            }
            if (best == null || hint.columns.size() > best.columns.size()) {
                best = hint;
            }
        }
        return best == null ? null : best.indexName;
    }

    private String readIdentifier() {
        if (pos >= sql.length || !isIdentifierStart(sql[pos])) {
            return null;
        }
        int start = pos++;
        while (pos < sql.length && isIdentifierPart(sql[pos])) {
            pos++;
        }
        return new String(sql, start, pos - start);
    }

    private String peekIdentifier() {
        int saved = pos;
        String ident = readIdentifier();
        pos = saved;
        return ident;
    }

    private void skipWhitespace() {
        while (pos < sql.length && Character.isWhitespace(sql[pos])) {
            pos++;
        }
    }

    private void consumeChar() {
        switch (sql[pos]) {
            case '\'':
                pos = skipSingleQuotes(pos);
                break;
            case '"':
                pos = skipDoubleQuotes(pos);
                break;
            case '`':
                pos = skipBacktickQuotes(pos);
                break;
            case '-':
                pos = skipLineComment(pos);
                break;
            case '/':
                pos = skipBlockComment(pos);
                break;
            case '(':
                parenLevel++;
                pos++;
                break;
            case ')':
                if (parenLevel > 0) {
                    parenLevel--;
                }
                pos++;
                break;
            default:
                pos++;
                break;
        }
    }

    private static boolean isClauseKeyword(String ident) {
        if (ident == null) {
            return false;
        }

        switch (ident.length()) {
            case 4:
                return ident.equalsIgnoreCase("from") || ident.equalsIgnoreCase("join");
            case 5:
                return ident.equalsIgnoreCase("where")
                        || ident.equalsIgnoreCase("order")
                        || ident.equalsIgnoreCase("group")
                        || ident.equalsIgnoreCase("limit")
                        || ident.equalsIgnoreCase("union");
            case 6:
                return ident.equalsIgnoreCase("having") || ident.equalsIgnoreCase("except");
            case 9:
                return ident.equalsIgnoreCase("intersect");
            default:
                return false;
        }
    }

    private static boolean isIdentifierStart(char ch) {
        return Character.isJavaIdentifierStart(ch);
    }

    private static boolean isIdentifierPart(char ch) {
        return Character.isJavaIdentifierPart(ch);
    }

    private int skipSingleQuotes(int offset) {
        while (++offset < sql.length) {
            if (sql[offset] == '\\') {
                ++offset;
            } else if (sql[offset] == '\'') {
                return offset + 1;
            }
        }
        return sql.length;
    }

    private int skipDoubleQuotes(int offset) {
        while (++offset < sql.length && sql[offset] != '"') {
        }
        return Math.min(offset + 1, sql.length);
    }

    private int skipBacktickQuotes(int offset) {
        while (++offset < sql.length && sql[offset] != '`') {
        }
        return Math.min(offset + 1, sql.length);
    }

    private int skipLineComment(int offset) {
        if (offset + 1 < sql.length && sql[offset + 1] == '-') {
            offset += 2;
            while (offset < sql.length && sql[offset] != '\r' && sql[offset] != '\n') {
                offset++;
            }
            return offset;
        }
        return offset + 1;
    }

    private int skipBlockComment(int offset) {
        if (offset + 1 >= sql.length || sql[offset + 1] != '*') {
            return offset + 1;
        }
        int level = 1;
        for (offset += 2; offset < sql.length; ++offset) {
            if (sql[offset - 1] == '*' && sql[offset] == '/') {
                --level;
                ++offset;
            } else if (sql[offset - 1] == '/' && sql[offset] == '*') {
                ++level;
                ++offset;
            }
            if (level == 0) {
                return offset;
            }
        }
        return sql.length;
    }

    private static final class TableRef {
        final String table;
        final String alias;
        final int afterTable;
        final int aliasStart;

        TableRef(String table, String alias, int afterTable, int aliasStart) {
            this.table = table;
            this.alias = alias;
            this.afterTable = afterTable;
            this.aliasStart = aliasStart;
        }
    }
}
