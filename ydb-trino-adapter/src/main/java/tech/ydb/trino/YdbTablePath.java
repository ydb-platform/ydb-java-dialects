package tech.ydb.trino;

import io.trino.spi.TrinoException;

import java.util.Locale;
import java.util.Optional;

import static io.trino.spi.StandardErrorCode.INVALID_ARGUMENTS;

/** A table path relative to the configured database, exposed in the default schema. */
record YdbTablePath(String value)
{
    static YdbTablePath fromUserInput(String value)
    {
        return lookup(value).orElseThrow(() -> new TrinoException(INVALID_ARGUMENTS,
                "Invalid YDB table path '" + value + "': expected a relative path without empty, '.' or '..' components"));
    }

    static Optional<YdbTablePath> lookup(String value)
    {
        for (String component : value.split("/", -1)) {
            if (component.isEmpty() || component.equals(".") || component.equals("..")) {
                return Optional.empty();
            }
        }
        return Optional.of(new YdbTablePath(value));
    }

    String comparisonKey()
    {
        return value.toLowerCase(Locale.ROOT);
    }
}
