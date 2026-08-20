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
    private static final Pattern COMPONENT = Pattern.compile("[A-Za-z0-9._-]+");

    static YdbTablePath fromUserInput(String value)
    {
        return parseUserInput(value);
    }

    static Optional<YdbTablePath> fromRemoteMetadata(String value)
    {
        return parse(value, false);
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
            throw invalidPath(value, validatePath(value), userInput);
        }

        String[] components = value.split("/", -1);
        if (!userInput) {
            for (String component : components) {
                if (component.startsWith(".")) {
                    return Optional.empty();
                }
            }
        }

        String pathError = validatePath(value);
        if (pathError != null) {
            throw invalidPath(value, pathError, userInput);
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

    private static String validatePath(String value)
    {
        if (value == null) {
            return "path must not be null";
        }
        if (value.length() > MAX_COMPONENT_LENGTH) {
            return "path is too long; must be at most " + MAX_COMPONENT_LENGTH + " characters";
        }
        return null;
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
