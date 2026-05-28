package tech.ydb.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.Ordered;

public class YdbTransactionInterceptorReplacer
        implements BeanDefinitionRegistryPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(YdbTransactionInterceptorReplacer.class);

    private static final String TRANSACTION_INTERCEPTOR_BEAN_NAME = "transactionInterceptor";

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
            throws BeansException {
        if (!registry.containsBeanDefinition(TRANSACTION_INTERCEPTOR_BEAN_NAME)) {
            if (log.isDebugEnabled()) {
                log.debug("BeanDefinition '" + TRANSACTION_INTERCEPTOR_BEAN_NAME + "' not found");
            }
            return;
        }

        BeanDefinition existingBd = registry.getBeanDefinition(TRANSACTION_INTERCEPTOR_BEAN_NAME);

        if (YdbTransactionInterceptorFactory.class.getName().equals(existingBd.getBeanClassName())) {
            if (log.isDebugEnabled()) {
                log.debug("BeanDefinition '" + TRANSACTION_INTERCEPTOR_BEAN_NAME
                        + "' is already YdbTransactionInterceptorFactory");
            }
            return;
        }

        AbstractBeanDefinition newBd = buildYdbInterceptorBeanDefinition(existingBd);

        registry.removeBeanDefinition(TRANSACTION_INTERCEPTOR_BEAN_NAME);
        registry.registerBeanDefinition(TRANSACTION_INTERCEPTOR_BEAN_NAME, newBd);

        if (log.isDebugEnabled()) {
            log.debug("registered YdbTransactionInterceptorFactory as bean '"
                    + TRANSACTION_INTERCEPTOR_BEAN_NAME + "'");
        }
    }

    private AbstractBeanDefinition buildYdbInterceptorBeanDefinition(BeanDefinition existingBd) {
        AbstractBeanDefinition newBd =
                BeanDefinitionBuilder.genericBeanDefinition(YdbTransactionInterceptorFactory.class)
                        .setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE)
                        .getBeanDefinition();

        copyBeanDefinitionMetadata(existingBd, newBd);
        return newBd;
    }

    private void copyBeanDefinitionMetadata(BeanDefinition source, AbstractBeanDefinition target) {
        target.setParentName(source.getParentName());
        target.setRole(source.getRole());
        target.setScope(source.getScope());
        target.setLazyInit(source.isLazyInit());
        target.setPrimary(source.isPrimary());
        target.setFallback(source.isFallback());
        target.setDependsOn(source.getDependsOn());
        target.setDescription(source.getDescription());
        target.setSource(source.getSource());

        if (source instanceof AbstractBeanDefinition abstractSource) {
            target.setAutowireCandidate(abstractSource.isAutowireCandidate());
            target.setDefaultCandidate(abstractSource.isDefaultCandidate());
            target.setSynthetic(abstractSource.isSynthetic());
            target.setResource(abstractSource.getResource());
            target.setResourceDescription(abstractSource.getResourceDescription());
            if (abstractSource.getOriginatingBeanDefinition() != null) {
                target.setOriginatingBeanDefinition(abstractSource.getOriginatingBeanDefinition());
            }
            target.copyQualifiersFrom(abstractSource);

            for (String attributeName : abstractSource.attributeNames()) {
                target.setAttribute(attributeName, abstractSource.getAttribute(attributeName));
            }
        }
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
