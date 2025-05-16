package tech.ydb.data.core.convert.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import tech.ydb.data.core.convert.YQLType;


/**
 * The annotation for qualification of the target YDB data type.
 *
 * @author Mikhail Polivakha
 * @author Aleksandr Gorshenin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface YdbType {
    /**
     * The target YDB data type.
     * @return The target YDB data type.
     */
    YQLType value();

    /**
     * Decimal precision. Applies only to {@link YQLType#Decimal }
     * @return Custom decimal type precision.
     */
    int decimalPrecision() default 22;

    /**
     * Decimal scale. Applies only to {@link YQLType#Decimal }
     * @return Custom decimal type scale.
     */
    int decimalScale() default 9;
}
