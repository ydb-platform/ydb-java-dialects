package tech.ydb.data.repository.config;

import java.util.Arrays;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.util.ReflectionUtils;

import tech.ydb.data.repository.ViewIndex;

/**
 * Enabling the exposure of the metadata for the {@link JdbcRepositoryFactoryBean}. Enables
 * only for those {@link RepositoryFactoryBeanSupport factory beans} that have any {@link ViewIndex}
 * annotated methods.
 *
 * @author Mikhail Polivakha
 */
@SuppressWarnings("rawtypes")
public class JdbcRepositoryBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof JdbcRepositoryFactoryBean rfbs && hasAnyViewIndexMethods(rfbs)) {
            rfbs.setExposeMetadata(true);
        }
        return bean;
    }

    /**
     * Unfortunately, {@link JdbcRepositoryFactoryBean#getRepositoryInformation()} call is not possible at this stage, since
     * {@link RepositoryFactoryBeanSupport#factory} is not initialized at this point yet. Still, we have
     * to use {@link BeanPostProcessor#postProcessBeforeInitialization(Object, String)} since the
     * {@link RepositoryFactoryBeanSupport#setExposeMetadata(boolean) expose metadata} call needs to be done before the {@link InitializingBean#afterPropertiesSet()}
     * to be propagated into the underlying {@link org.springframework.data.repository.core.support.RepositoryFactorySupport factory support}
     */
    private static boolean hasAnyViewIndexMethods(JdbcRepositoryFactoryBean rfbs) {
        return Arrays
          .stream(ReflectionUtils.getAllDeclaredMethods(rfbs.getObjectType()))
          .anyMatch(method -> AnnotationUtils.getAnnotation(method, ViewIndex.class) != null);
    }
}
