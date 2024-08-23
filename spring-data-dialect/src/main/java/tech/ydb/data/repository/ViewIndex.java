package tech.ydb.data.repository;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Kirill Kurdyukov
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ViewIndex {

    String name();
}
