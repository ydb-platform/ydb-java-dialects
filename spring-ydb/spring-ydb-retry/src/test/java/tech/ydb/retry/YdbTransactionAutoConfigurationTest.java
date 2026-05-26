package tech.ydb.retry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;

class YdbTransactionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(YdbTransactionAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldNotRegisterRetryWrapperWhenRetryDisabled() {
        contextRunner
                .withPropertyValues("ydb.transaction.retry.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(TransactionInterceptor.class);
                    assertThat(context).doesNotHaveBean(YdbTransactionInterceptorReplacer.class);
                    assertThat(context.getBean("transactionInterceptor"))
                            .isInstanceOf(TransactionInterceptor.class)
                            .isNotInstanceOf(YdbTransactionInterceptor.class);
                });
    }

    @Test
    void shouldReplaceTransactionInterceptorWhenRetryEnabled() {
        contextRunner
                .withPropertyValues("ydb.transaction.retry.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(TransactionInterceptor.class);
                    assertThat(context).hasSingleBean(YdbTransactionInterceptorReplacer.class);
                    assertThat(context.getBean("transactionInterceptor"))
                            .isInstanceOf(YdbTransactionInterceptor.class);
                });
    }

    @Test
    void shouldReplaceTransactionInterceptorWhenRetryPropertyMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TransactionInterceptor.class);
            assertThat(context).hasSingleBean(YdbTransactionInterceptorReplacer.class);
            assertThat(context.getBean("transactionInterceptor"))
                    .isInstanceOf(YdbTransactionInterceptor.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new RecordingTransactionManager();
        }

        @Bean
        TestService testService() {
            return new TestService();
        }
    }

    static class TestService {

        @Transactional
        public void work() {
        }
    }

    static final class RecordingTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
