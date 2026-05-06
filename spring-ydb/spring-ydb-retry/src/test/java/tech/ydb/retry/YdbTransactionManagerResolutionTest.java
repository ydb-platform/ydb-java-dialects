package tech.ydb.retry;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class YdbTransactionManagerResolutionTest {

    @Test
    void shouldUseSingleManager() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SingleManagerConfig.class)) {
            SingleManagerService service = context.getBean(SingleManagerService.class);
            RecordingTransactionManager manager = context.getBean("singleManager", RecordingTransactionManager.class);

            service.defaultOperation();

            assertEquals(1, manager.beginCount());
            assertEquals(1, manager.commitCount());
            assertEquals(0, manager.rollbackCount());
            assertInstanceOf(YdbTransactionInterceptor.class, context.getBean("transactionInterceptor"));
            assertEquals(1, context.getBeansOfType(TransactionInterceptor.class).size());
        }
    }

    @Test
    void shouldResolveExplicitTransactionManagersWithoutPrimary() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MultiManagerConfig.class)) {
            MultiManagerService service = context.getBean(MultiManagerService.class);
            RecordingTransactionManager ydbManager = context.getBean("ydbTransactionManager", RecordingTransactionManager.class);
            RecordingTransactionManager auditManager = context.getBean("auditTransactionManager", RecordingTransactionManager.class);

            service.ydbOperation();

            assertEquals(1, ydbManager.beginCount());
            assertEquals(1, ydbManager.commitCount());
            assertEquals(0, ydbManager.rollbackCount());
            assertEquals(0, auditManager.beginCount());
            assertEquals(0, auditManager.commitCount());
            assertEquals(0, auditManager.rollbackCount());

            ydbManager.reset();
            auditManager.reset();

            service.auditOperation();

            assertEquals(0, ydbManager.beginCount());
            assertEquals(0, ydbManager.commitCount());
            assertEquals(0, ydbManager.rollbackCount());
            assertEquals(1, auditManager.beginCount());
            assertEquals(1, auditManager.commitCount());
            assertEquals(0, auditManager.rollbackCount());
            assertInstanceOf(YdbTransactionInterceptor.class, context.getBean("transactionInterceptor"));
            assertEquals(1, context.getBeansOfType(TransactionInterceptor.class).size());
        }
    }

    @Test
    void shouldUseConfigurerDefaultTransactionManager() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConfigurerDefaultManagerConfig.class)) {
            ConfigurerDefaultManagerService service = context.getBean(ConfigurerDefaultManagerService.class);
            RecordingTransactionManager ydbManager = context.getBean("ydbTransactionManager", RecordingTransactionManager.class);
            RecordingTransactionManager auditManager = context.getBean("auditTransactionManager", RecordingTransactionManager.class);

            service.defaultSpringOperation();

            assertEquals(0, ydbManager.beginCount());
            assertEquals(0, ydbManager.commitCount());
            assertEquals(1, auditManager.beginCount());
            assertEquals(1, auditManager.commitCount());

            ydbManager.reset();
            auditManager.reset();

            service.defaultYdbOperation();

            assertEquals(0, ydbManager.beginCount());
            assertEquals(0, ydbManager.commitCount());
            assertEquals(1, auditManager.beginCount());
            assertEquals(1, auditManager.commitCount());
        }
    }

    @Test
    void shouldUsePrimaryTransactionManager() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(PrimaryManagerConfig.class)) {
            PrimaryManagerService service = context.getBean(PrimaryManagerService.class);
            RecordingTransactionManager primaryManager = context.getBean("primaryTransactionManager", RecordingTransactionManager.class);
            RecordingTransactionManager secondaryManager = context.getBean("secondaryTransactionManager", RecordingTransactionManager.class);

            service.defaultSpringOperation();

            assertEquals(1, primaryManager.beginCount());
            assertEquals(1, primaryManager.commitCount());
            assertEquals(0, secondaryManager.beginCount());
            assertEquals(0, secondaryManager.commitCount());

            primaryManager.reset();
            secondaryManager.reset();

            service.defaultYdbOperation();

            assertEquals(1, primaryManager.beginCount());
            assertEquals(1, primaryManager.commitCount());
            assertEquals(0, secondaryManager.beginCount());
            assertEquals(0, secondaryManager.commitCount());
        }
    }

    @Test
    void ydbTransactionalAliasShouldExposeTransactionManagerQualifier() throws NoSuchMethodException {
        AnnotationTransactionAttributeSource attributeSource = new AnnotationTransactionAttributeSource();
        Method ydbMethod = MultiManagerService.class.getMethod("ydbOperation");
        Method auditMethod = MultiManagerService.class.getMethod("auditOperation");

        TransactionAttribute ydbAttribute = attributeSource.getTransactionAttribute(ydbMethod, MultiManagerService.class);
        TransactionAttribute auditAttribute = attributeSource.getTransactionAttribute(auditMethod, MultiManagerService.class);

        assertNotNull(ydbAttribute);
        assertNotNull(auditAttribute);
        assertEquals("ydbTransactionManager", ydbAttribute.getQualifier());
        assertEquals("auditTransactionManager", auditAttribute.getQualifier());
    }

    @Test
    void ydbTransactionalValueAliasShouldExposeTransactionManagerQualifier() throws NoSuchMethodException {
        AnnotationTransactionAttributeSource attributeSource = new AnnotationTransactionAttributeSource();
        Method method = MultiManagerService.class.getMethod("ydbValueAliasOperation");

        TransactionAttribute attribute = attributeSource.getTransactionAttribute(method, MultiManagerService.class);

        assertNotNull(attribute);
        assertEquals("ydbTransactionManager", attribute.getQualifier());
    }

    @Test
    void ydbTransactionalTimeoutStringShouldExposeTimeout() throws NoSuchMethodException {
        AnnotationTransactionAttributeSource attributeSource = new AnnotationTransactionAttributeSource();
        Method method = MultiManagerService.class.getMethod("ydbTimeoutStringOperation");

        TransactionAttribute attribute = attributeSource.getTransactionAttribute(method, MultiManagerService.class);

        assertNotNull(attribute);
        assertEquals(15, attribute.getTimeout());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import(YdbTransactionAutoConfiguration.class)
    static class SingleManagerConfig {

        @Bean("singleManager")
        RecordingTransactionManager singleManager() {
            return new RecordingTransactionManager();
        }

        @Bean
        SingleManagerService singleManagerService() {
            return new SingleManagerService();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import(YdbTransactionAutoConfiguration.class)
    static class MultiManagerConfig {

        @Bean("ydbTransactionManager")
        RecordingTransactionManager ydbTransactionManager() {
            return new RecordingTransactionManager();
        }

        @Bean("auditTransactionManager")
        RecordingTransactionManager auditTransactionManager() {
            return new RecordingTransactionManager();
        }

        @Bean
        MultiManagerService multiManagerService() {
            return new MultiManagerService();
        }
    }

    @Configuration
    @EnableTransactionManagement
    @Import(YdbTransactionAutoConfiguration.class)
    static class ConfigurerDefaultManagerConfig implements TransactionManagementConfigurer {

        @Bean("ydbTransactionManager")
        RecordingTransactionManager ydbTransactionManager() {
            return new RecordingTransactionManager();
        }

        @Bean("auditTransactionManager")
        RecordingTransactionManager auditTransactionManager() {
            return new RecordingTransactionManager();
        }

        @Bean
        ConfigurerDefaultManagerService configurerDefaultManagerService() {
            return new ConfigurerDefaultManagerService();
        }

        @Override
        public @NotNull TransactionManager annotationDrivenTransactionManager() {
            return auditTransactionManager();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import(YdbTransactionAutoConfiguration.class)
    static class PrimaryManagerConfig {

        @Bean("primaryTransactionManager")
        @Primary
        RecordingTransactionManager primaryTransactionManager() {
            return new RecordingTransactionManager();
        }

        @Bean("secondaryTransactionManager")
        RecordingTransactionManager secondaryTransactionManager() {
            return new RecordingTransactionManager();
        }

        @Bean
        PrimaryManagerService primaryManagerService() {
            return new PrimaryManagerService();
        }
    }

    static class SingleManagerService {

        @Transactional
        public void defaultOperation() {
        }
    }

    static class MultiManagerService {

        @YdbTransactional(transactionManager = "ydbTransactionManager")
        public void ydbOperation() {
        }

        @YdbTransactional("ydbTransactionManager")
        public void ydbValueAliasOperation() {
        }

        @YdbTransactional(timeoutString = "15")
        public void ydbTimeoutStringOperation() {
        }

        @Transactional(transactionManager = "auditTransactionManager")
        public void auditOperation() {
        }
    }

    static class ConfigurerDefaultManagerService {

        @Transactional
        public void defaultSpringOperation() {
        }

        @YdbTransactional
        public void defaultYdbOperation() {
        }
    }

    static class PrimaryManagerService {

        @Transactional
        public void defaultSpringOperation() {
        }

        @YdbTransactional
        public void defaultYdbOperation() {
        }
    }

    static final class RecordingTransactionManager implements PlatformTransactionManager {
        private final AtomicInteger beginCount = new AtomicInteger();
        private final AtomicInteger commitCount = new AtomicInteger();
        private final AtomicInteger rollbackCount = new AtomicInteger();

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            beginCount.incrementAndGet();
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            commitCount.incrementAndGet();
        }

        @Override
        public void rollback(TransactionStatus status) {
            rollbackCount.incrementAndGet();
        }

        int beginCount() {
            return beginCount.get();
        }

        int commitCount() {
            return commitCount.get();
        }

        int rollbackCount() {
            return rollbackCount.get();
        }

        void reset() {
            beginCount.set(0);
            commitCount.set(0);
            rollbackCount.set(0);
        }
    }
}
