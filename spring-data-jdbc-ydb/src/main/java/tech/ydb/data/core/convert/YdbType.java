package tech.ydb.data.core.convert;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import tech.ydb.table.values.PrimitiveType;

/**
 * @author Madiyar Nurgazin
 * @author Mikhail Polivakha
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface YdbType {
    String value();
}
