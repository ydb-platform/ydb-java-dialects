package tech.ydb.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@AutoConfiguration
@ConditionalOnClass(TransactionInterceptor.class)
@EnableConfigurationProperties(YdbRetryProperties.class)
@ConditionalOnProperty(prefix = "ydb.transaction.retry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class YdbTransactionAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(YdbTransactionAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public static YdbTransactionInterceptorReplacer ydbTransactionInterceptorReplacer() {
        log.debug("creating YdbTransactionInterceptorReplacer bean");
        return new YdbTransactionInterceptorReplacer();
    }
}
