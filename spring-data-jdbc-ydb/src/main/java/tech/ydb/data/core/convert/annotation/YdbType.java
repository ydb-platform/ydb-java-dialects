package tech.ydb.data.core.convert.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import tech.ydb.table.values.PrimitiveType;

/**
 * The annotation for qualification of the target YDB data type.
 *
 * @author Mikhail Polivakha
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface YdbType {

    /**
     * The target YDB data type.
     */
    PrimitiveType value();
}
