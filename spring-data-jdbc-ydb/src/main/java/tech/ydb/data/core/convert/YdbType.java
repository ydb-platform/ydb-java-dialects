package tech.ydb.data.core.convert;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation for qualification of the target YDB data type.
 *
 * @author Madiyar Nurgazin
 * @author Mikhail Polivakha
 * @deprecated Please, use {@link tech.ydb.data.core.convert.annotation.YdbType} instead because of type safety considerations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Deprecated(forRemoval = true)
@Documented
public @interface YdbType {
    /**
     * The target YDB data type.
     * @return name of YDB data type
     */
    String value();
}
