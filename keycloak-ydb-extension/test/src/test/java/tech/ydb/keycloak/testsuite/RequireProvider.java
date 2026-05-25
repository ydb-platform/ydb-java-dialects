package tech.ydb.keycloak.testsuite;

import org.keycloak.provider.Provider;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(RequireProviders.class)
public @interface RequireProvider {
    Class<? extends Provider> value() default Provider.class;

    String[] only() default {};

    String[] exclude() default {};
}
