package tech.ydb.retry;

import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.ProxyMethodInvocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.ydb.core.StatusCode.BAD_SESSION;

class YdbTransactionInterceptorInvocationTest extends InterceptorTestSupport {

    @Test
    void shouldCloneProxyMethodInvocationForEachRetryAttempt() throws Throwable {
        TestableInterceptor interceptor = interceptorWithConfig(true, 3, 0, 0, 0, 0);

        Method method = methodOf("ydbCustomRetry");
        Object target = new YdbTransactionalTestService();

        ProxyMethodInvocation invocation = Mockito.mock(ProxyMethodInvocation.class);
        MethodInvocation firstAttempt = Mockito.mock(MethodInvocation.class);
        MethodInvocation secondAttempt = Mockito.mock(MethodInvocation.class);

        stubInvocationMetadata(invocation, method, target);
        stubInvocationMetadata(firstAttempt, method, target);
        stubInvocationMetadata(secondAttempt, method, target);

        Mockito.when(invocation.proceed())
                .thenThrow(new AssertionError("original invocation must not be proceeded directly"));
        Mockito.when(invocation.invocableClone()).thenReturn(firstAttempt, secondAttempt);
        Mockito.when(firstAttempt.proceed()).thenThrow(new ConfigurableStatusException(BAD_SESSION));
        Mockito.when(secondAttempt.proceed()).thenReturn("ok");

        interceptor.enqueueOutcome(
                new ConfigurableStatusException(BAD_SESSION), "ok");
        Object result = interceptor.invoke(invocation);

        assertEquals("ok", result);
        assertEquals(2, interceptor.allInvocations());
        Mockito.verify(invocation, Mockito.times(2)).invocableClone();
        Mockito.verify(invocation, Mockito.never()).proceed();
        Mockito.verify(firstAttempt).proceed();
        Mockito.verify(secondAttempt).proceed();
    }

    private static void stubInvocationMetadata(MethodInvocation invocation, Method method, Object target) {
        Mockito.when(invocation.getMethod()).thenReturn(method);
        Mockito.when(invocation.getThis()).thenReturn(target);
        Mockito.when(invocation.getArguments()).thenReturn(new Object[0]);
    }
}
