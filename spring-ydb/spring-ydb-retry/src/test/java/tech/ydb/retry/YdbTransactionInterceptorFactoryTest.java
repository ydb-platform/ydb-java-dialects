package tech.ydb.retry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YdbTransactionInterceptorFactoryTest {

    @Test
    void getObjectTypeShouldReturnYdbTransactionInterceptorClass() {
        YdbTransactionInterceptorFactory factory = new YdbTransactionInterceptorFactory();

        assertEquals(YdbTransactionInterceptor.class, factory.getObjectType());
    }

    @Test
    void getObjectShouldReturnYdbTransactionInterceptor() {
        YdbTransactionInterceptorFactory factory = createYdbTransactionInterceptorFactory();
        YdbTransactionInterceptor interceptor = factory.getObject();

        assertNotNull(interceptor);
    }

    @Test
    void getObjectShouldUseRetryPropertiesConfig() {
        YdbRetryProperties properties = new YdbRetryProperties();
        properties.setEnabled(false);
        properties.setMaxRetries(3);
        YdbTransactionInterceptorFactory factory = new YdbTransactionInterceptorFactory();
        factory.setRetryProperties(properties);
        factory.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());

        assertNotNull(factory.getObject());
    }

    @Test
    void getObjectShouldThrowNpeWhenRetryPropertiesIsNull() {
        YdbTransactionInterceptorFactory factory = new YdbTransactionInterceptorFactory();
        factory.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());

        assertThrows(NullPointerException.class, factory::getObject);
    }

    @Test
    void getObjectShouldSetTransactionAttributeSource() {
        TransactionAttributeSource tas = new AnnotationTransactionAttributeSource();
        YdbTransactionInterceptorFactory factory = new YdbTransactionInterceptorFactory();
        factory.setRetryProperties(new YdbRetryProperties());
        factory.setTransactionAttributeSource(tas);

        YdbTransactionInterceptor interceptor = factory.getObject();

        assertNotNull(interceptor);
        assertSame(tas, interceptor.getTransactionAttributeSource());
    }

    @Test
    void getObjectShouldLeaveTransactionManagerUnsetForDeferredResolution() {
        YdbTransactionInterceptorFactory factory = createYdbTransactionInterceptorFactory();

        YdbTransactionInterceptor interceptor = factory.getObject();

        assertNotNull(interceptor);
        assertNull(interceptor.getTransactionManager());
    }

    @Test
    void getObjectShouldCreateInterceptorWhenBeanFactoryIsProvided() {
        YdbTransactionInterceptorFactory factory = createYdbTransactionInterceptorFactory();
        factory.setBeanFactory(new DefaultListableBeanFactory());

        YdbTransactionInterceptor interceptor = factory.getObject();

        assertNotNull(interceptor);
    }

    @Test
    void getObjectShouldCreateNewInstanceOnEachCall() {
        YdbTransactionInterceptorFactory factory = createYdbTransactionInterceptorFactory();

        YdbTransactionInterceptor first = factory.getObject();
        YdbTransactionInterceptor second = factory.getObject();

        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second);
    }

    private static YdbTransactionInterceptorFactory createYdbTransactionInterceptorFactory() {
        YdbTransactionInterceptorFactory factory = new YdbTransactionInterceptorFactory();
        factory.setRetryProperties(new YdbRetryProperties());
        factory.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        return factory;
    }
}
