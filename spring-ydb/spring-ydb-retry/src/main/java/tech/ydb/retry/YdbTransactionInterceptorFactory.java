package tech.ydb.retry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

public class YdbTransactionInterceptorFactory
        implements FactoryBean<YdbTransactionInterceptor>, BeanFactoryAware {

    private YdbRetryProperties retryProperties;
    private TransactionAttributeSource transactionAttributeSource;

    @Nullable
    private BeanFactory beanFactory;

    public void setRetryProperties(YdbRetryProperties retryProperties) {
        this.retryProperties = retryProperties;
    }

    public void setTransactionAttributeSource(TransactionAttributeSource transactionAttributeSource) {
        this.transactionAttributeSource = transactionAttributeSource;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public YdbTransactionInterceptor getObject() {
        YdbTransactionInterceptor interceptor = new YdbTransactionInterceptor(retryProperties.toConfig(), Thread::sleep);
        interceptor.setTransactionAttributeSource(transactionAttributeSource);
        if (beanFactory != null) {
            interceptor.setBeanFactory(beanFactory);
        }

        TransactionManager defaultTransactionManager = resolveTransactionManager();
        if (defaultTransactionManager != null) {
            interceptor.setTransactionManager(defaultTransactionManager);
        }

        return interceptor;
    }

    @Nullable
    private TransactionManager resolveTransactionManager() {
        if (beanFactory == null) {
            return null;
        }

        TransactionManagementConfigurer configurer = beanFactory.getBeanProvider(TransactionManagementConfigurer.class).getIfAvailable();
        if (configurer == null) {
            return null;
        }

        return configurer.annotationDrivenTransactionManager();
    }

    @Override
    public Class<?> getObjectType() {
        return YdbTransactionInterceptor.class;
    }
}
