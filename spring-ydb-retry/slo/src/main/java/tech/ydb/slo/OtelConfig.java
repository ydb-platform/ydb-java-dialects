package tech.ydb.slo;

import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OtelConfig {

    private static final int PROMETHEUS_PORT = 9464;

    @Bean(destroyMethod = "close")
    public OpenTelemetrySdk openTelemetry() {
        PrometheusHttpServer prometheusHttpServer =
                PrometheusHttpServer.builder().setPort(PROMETHEUS_PORT).build();

        SdkMeterProvider meterProvider =
                SdkMeterProvider.builder().registerMetricReader(prometheusHttpServer).build();

        return OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build();
    }
}
