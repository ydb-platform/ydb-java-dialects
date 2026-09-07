package tech.ydb.trino;

import io.trino.spi.ErrorCodeSupplier;
import io.trino.spi.TrinoException;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import static io.trino.plugin.jdbc.JdbcErrorCode.JDBC_ERROR;
import static io.trino.spi.StandardErrorCode.INVALID_ARGUMENTS;

/**
 * A validated YDB object path relative to the configured database root.
 * <p>
 * The path is the complete Trino table name inside the virtual {@code default} schema.
 * Components follow YDB naming limits: at most 255 ASCII letters, digits, '.', '_' or '-';
 * a relative path contains at most 32 components. The connector additionally excludes
 * dot-prefixed objects and rejects absolute paths, empty components and traversal.
 */
record YdbTablePath(String value)
{
    private static final int MAX_COMPONENT_LENGTH = 255;
    private static final int MAX_PATH_DEPTH = 32;
    private static final Pattern COMPONENT = Pattern.compile("[A-Za-z0-9._-]+");

    /**
     * Parses a name that will address or create a YDB object. An invalid name is a
     * caller error and fails with {@code INVALID_ARGUMENTS}.
     */
    static YdbTablePath fromUserInput(String value)
    {
        String reason = invalidReason(value);
        if (reason != null) {
            throw invalidPath(value, reason, INVALID_ARGUMENTS);
        }
        return new YdbTablePath(value);
    }

    /**
     * Names outside the exposed namespace resolve to no table during lookup.
     */
    static Optional<YdbTablePath> lookup(String value)
    {
        if (invalidReason(value) != null) {
            return Optional.empty();
        }
        return Optional.of(new YdbTablePath(value));
    }

    /**
     * Parses a path reported by JDBC metadata. Paths with a dot-prefixed component are
     * system or hidden objects and are not published. Any other malformed path is a driver
     * or server inconsistency and fails with {@code JDBC_ERROR}.
     */
    static Optional<YdbTablePath> fromRemoteMetadata(String value)
    {
        if (value != null && hasDotPrefixedComponent(value)) {
            return Optional.empty();
        }
        String reason = invalidReason(value);
        if (reason != null) {
            throw invalidPath(value, reason, JDBC_ERROR);
        }
        return Optional.of(new YdbTablePath(value));
    }

    String comparisonKey()
    {
        return value.toLowerCase(Locale.ROOT);
    }

    private static boolean hasDotPrefixedComponent(String value)
    {
        for (String component : value.split("/", -1)) {
            if (component.startsWith(".")) {
                return true;
            }
        }
        return false;
    }

    private static String invalidReason(String value)
    {
        if (value == null) {
            return "path must not be null";
        }
        String[] components = value.split("/", -1);
        if (components.length > MAX_PATH_DEPTH) {
            return "paths are too deep; must contain at most " + MAX_PATH_DEPTH + " components";
        }
        for (String component : components) {
            if (component.isEmpty()) {
                return "components must not be empty";
            }
            if (component.startsWith(".")) {
                return "components must not start with '.'";
            }
            if (component.length() > MAX_COMPONENT_LENGTH) {
                return "components are too long; must be at most " + MAX_COMPONENT_LENGTH + " characters";
            }
            if (!COMPONENT.matcher(component).matches()) {
                return "components may contain only letters, digits, '.', '_' and '-'";
            }
        }
        return null;
    }

    private static TrinoException invalidPath(String value, String reason, ErrorCodeSupplier errorCode)
    {
        return new TrinoException(errorCode, "Invalid YDB table path '" + value + "': " + reason);
    }
}
