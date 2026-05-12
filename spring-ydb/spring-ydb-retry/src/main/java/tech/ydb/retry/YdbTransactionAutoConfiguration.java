package tech.ydb.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@AutoConfiguration
@ConditionalOnClass(TransactionInterceptor.class)
@EnableConfigurationProperties(YdbRetryProperties.class)
public class YdbTransactionAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(YdbTransactionAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public static YdbTransactionInterceptorReplacer ydbBeanDefinitionRegistryPostProcessor() {
        log.debug("creating YdbBeanDefinitionRegistryPostProcessor bean");
        return new YdbTransactionInterceptorReplacer();
    }
}
