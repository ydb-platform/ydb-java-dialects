package tech.ydb.trino;

import io.trino.spi.TrinoException;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import static io.trino.plugin.jdbc.JdbcErrorCode.JDBC_ERROR;
import static io.trino.spi.StandardErrorCode.INVALID_ARGUMENTS;

record YdbTablePath(String value)
{
    private static final int MAX_COMPONENT_LENGTH = 255;
    private static final int MAX_PATH_DEPTH = 32;
    private static final String DATA_SYSTEM_TABLE_SUFFIX = "$data";
    private static final Pattern COMPONENT = Pattern.compile("[A-Za-z0-9._-]+");

    static YdbTablePath fromUserInput(String value)
    {
        return parseUserInput(value);
    }

    static Optional<YdbTablePath> fromRemoteMetadata(String value)
    {
        return parse(value, false);
    }

    static boolean isDataSystemTableName(String value)
    {
        if (value == null || !value.endsWith(DATA_SYSTEM_TABLE_SUFFIX)) {
            return false;
        }
        String tablePath = value.substring(0, value.length() - DATA_SYSTEM_TABLE_SUFFIX.length());
        if (tablePath.isEmpty()) {
            return false;
        }
        try {
            fromUserInput(tablePath);
            return true;
        } catch (TrinoException e) {
            return false;
        }
    }

    String comparisonKey()
    {
        return value.toLowerCase(Locale.ROOT);
    }

    private static YdbTablePath parseUserInput(String value)
    {
        return parse(value, true).orElseThrow();
    }

    private static Optional<YdbTablePath> parse(String value, boolean userInput)
    {
        if (value == null) {
            throw invalidPath(value, "path must not be null", userInput);
        }

        String[] components = value.split("/", -1);
        if (!userInput) {
            for (String component : components) {
                if (component.startsWith(".")) {
                    return Optional.empty();
                }
            }
        }
        if (components.length > MAX_PATH_DEPTH) {
            throw invalidPath(value, "paths are too deep; must contain at most " + MAX_PATH_DEPTH + " components", userInput);
        }

        for (String component : components) {
            if (component.startsWith(".")) {
                throw invalidPath(value, "components must not start with '.'", true);
            }

            String componentError = validateComponent(component);
            if (componentError != null) {
                throw invalidPath(value, componentError, userInput);
            }
        }
        return Optional.of(new YdbTablePath(value));
    }

    private static String validateComponent(String component)
    {
        if (component.isEmpty()) {
            return "components must not be empty";
        }
        if (component.length() > MAX_COMPONENT_LENGTH) {
            return "components are too long; must be at most " + MAX_COMPONENT_LENGTH + " characters";
        }
        if (!COMPONENT.matcher(component).matches()) {
            return "components may contain only letters, digits, '.', '_' and '-'";
        }
        return null;
    }

    private static TrinoException invalidPath(String value, String reason, boolean userInput)
    {
        return new TrinoException(
                userInput ? INVALID_ARGUMENTS : JDBC_ERROR,
                "Invalid YDB table path '" + value + "': " + reason);
    }
}
