package tech.ydb.retry;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

class YdbTransactionInterceptorReplacerTest {

    @Test
    void shouldHaveLowestPrecedenceOrder() {
        YdbTransactionInterceptorReplacer pp = new YdbTransactionInterceptorReplacer();
        assertEquals(LOWEST_PRECEDENCE, pp.getOrder());
    }

    @Test
    void shouldSkipWhenTransactionInterceptorNotFound() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        YdbTransactionInterceptorReplacer pp = new YdbTransactionInterceptorReplacer();
        pp.postProcessBeanDefinitionRegistry(beanFactory);

        assertFalse(beanFactory.containsBeanDefinition("transactionInterceptor"));
    }

    @Test
    void shouldSkipWhenAlreadyYdbTransactionInterceptorFactory() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        BeanDefinition beanDefinition =
                BeanDefinitionBuilder.genericBeanDefinition(YdbTransactionInterceptorFactory.class)
                        .getBeanDefinition();
        beanFactory.registerBeanDefinition("transactionInterceptor", beanDefinition);

        YdbTransactionInterceptorReplacer pp = new YdbTransactionInterceptorReplacer();
        pp.postProcessBeanDefinitionRegistry(beanFactory);

        String beanClassName =
                beanFactory.getBeanDefinition("transactionInterceptor").getBeanClassName();
        assertEquals(YdbTransactionInterceptorFactory.class.getName(), beanClassName);
    }

    @Test
    void shouldReplaceStandardTransactionInterceptorBeanDefinition() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        BeanDefinition beanDefinition =
                BeanDefinitionBuilder.genericBeanDefinition(TransactionInterceptor.class)
                        .getBeanDefinition();
        beanFactory.registerBeanDefinition("transactionInterceptor", beanDefinition);

        PlatformTransactionManager txManager = Mockito.mock(PlatformTransactionManager.class);
        YdbRetryProperties properties = new YdbRetryProperties();
        TransactionAttributeSource tas = new AnnotationTransactionAttributeSource();

        beanFactory.registerSingleton("transactionManager", txManager);
        beanFactory.registerSingleton(YdbRetryProperties.class.getName(), properties);
        beanFactory.registerSingleton(TransactionAttributeSource.class.getName(), tas);

        YdbTransactionInterceptorReplacer pp = new YdbTransactionInterceptorReplacer();
        pp.postProcessBeanDefinitionRegistry(beanFactory);

        beanDefinition = beanFactory.getBeanDefinition("transactionInterceptor");
        assertEquals(
                YdbTransactionInterceptorFactory.class.getName(), beanDefinition.getBeanClassName());

        Object bean = beanFactory.getBean("transactionInterceptor");
        assertInstanceOf(YdbTransactionInterceptor.class, bean);

        Map<String, TransactionInterceptor> interceptors =
                beanFactory.getBeansOfType(TransactionInterceptor.class);
        assertEquals(1, interceptors.size());
        assertSame(bean, interceptors.get("transactionInterceptor"));
    }

    @Test
    void shouldRegisterInterceptorWithCorrectProperties() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        BeanDefinition beanDefinition =
                BeanDefinitionBuilder.genericBeanDefinition(TransactionInterceptor.class)
                        .getBeanDefinition();
        beanFactory.registerBeanDefinition("transactionInterceptor", beanDefinition);

        PlatformTransactionManager txManager = Mockito.mock(PlatformTransactionManager.class);
        YdbRetryProperties properties = new YdbRetryProperties();
        properties.setEnabled(false);
        properties.setMaxAttempts(3);
        TransactionAttributeSource tas = new AnnotationTransactionAttributeSource();

        beanFactory.registerSingleton("transactionManager", txManager);
        beanFactory.registerSingleton(YdbRetryProperties.class.getName(), properties);
        beanFactory.registerSingleton(TransactionAttributeSource.class.getName(), tas);

        YdbTransactionInterceptorReplacer pp = new YdbTransactionInterceptorReplacer();
        pp.postProcessBeanDefinitionRegistry(beanFactory);

        Object bean = beanFactory.getBean("transactionInterceptor");
        assertInstanceOf(YdbTransactionInterceptor.class, bean);
    }

    @Test
    void shouldPreserveBeanDefinitionMetadataWhenReplacingInterceptor() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        AbstractBeanDefinition beanDefinition =
                BeanDefinitionBuilder.genericBeanDefinition(TransactionInterceptor.class)
                        .getBeanDefinition();
        beanDefinition.setPrimary(true);
        beanDefinition.setFallback(true);
        beanDefinition.setLazyInit(true);
        beanDefinition.setDependsOn("txDependency");
        beanDefinition.setParentName("txParent");
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        beanDefinition.setDescription("transaction interceptor");
        beanDefinition.setDefaultCandidate(false);
        beanDefinition.setSynthetic(true);
        beanDefinition.setInitMethodNames("initInterceptor");
        beanDefinition.setDestroyMethodNames("destroyInterceptor");
        beanDefinition.addQualifier(new AutowireCandidateQualifier(String.class));
        beanDefinition.setAttribute("preserveTargetClass", true);
        ByteArrayResource resource = new ByteArrayResource(new byte[0], "tx-resource");
        beanDefinition.setResource(resource);
        beanDefinition.setResourceDescription("tx-resource-description");
        BeanDefinition originatingBeanDefinition =
                BeanDefinitionBuilder.genericBeanDefinition(Object.class).getBeanDefinition();
        beanDefinition.setOriginatingBeanDefinition(originatingBeanDefinition);
        Object source = new Object();
        beanDefinition.setSource(source);
        beanFactory.registerBeanDefinition(
                "txParent", BeanDefinitionBuilder.genericBeanDefinition(Object.class).getBeanDefinition());
        beanFactory.registerBeanDefinition(
                "txDependency",
                BeanDefinitionBuilder.genericBeanDefinition(Object.class).getBeanDefinition());
        beanFactory.registerBeanDefinition("transactionInterceptor", beanDefinition);
        beanFactory.registerSingleton(YdbRetryProperties.class.getName(), new YdbRetryProperties());
        beanFactory.registerSingleton(
                TransactionAttributeSource.class.getName(), new AnnotationTransactionAttributeSource());

        YdbTransactionInterceptorReplacer pp = new YdbTransactionInterceptorReplacer();
        pp.postProcessBeanDefinitionRegistry(beanFactory);

        AbstractBeanDefinition replaced =
                (AbstractBeanDefinition) beanFactory.getBeanDefinition("transactionInterceptor");
        assertEquals(YdbTransactionInterceptorFactory.class.getName(), replaced.getBeanClassName());
        assertTrue(replaced.isPrimary());
        assertTrue(replaced.isFallback());
        assertTrue(replaced.isLazyInit());
        assertArrayEquals(new String[]{"txDependency"}, replaced.getDependsOn());
        assertEquals("txParent", replaced.getParentName());
        assertEquals(BeanDefinition.ROLE_INFRASTRUCTURE, replaced.getRole());
        assertEquals(BeanDefinition.SCOPE_PROTOTYPE, replaced.getScope());
        assertEquals("transaction interceptor", replaced.getDescription());
        assertFalse(replaced.isDefaultCandidate());
        assertTrue(replaced.isSynthetic());
        assertNull(replaced.getInitMethodNames());
        assertNull(replaced.getDestroyMethodNames());
        assertTrue(replaced.hasQualifier(String.class.getName()));
        assertEquals(true, replaced.getAttribute("preserveTargetClass"));
        assertEquals(beanDefinition.getResource().getClass(), replaced.getResource().getClass());
        assertEquals(
                beanDefinition.getResource().getDescription(), replaced.getResource().getDescription());
        assertEquals(beanDefinition.getResourceDescription(), replaced.getResourceDescription());
        assertSame(originatingBeanDefinition, replaced.getOriginatingBeanDefinition());
        assertSame(source, replaced.getSource());

        Object bean = beanFactory.getBean("transactionInterceptor");
        assertInstanceOf(YdbTransactionInterceptor.class, bean);
    }
}
