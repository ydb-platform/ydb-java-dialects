package tech.ydb.slo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SloConfig.class)
public class SloApplication {
    public static void main(String[] args) {
        SpringApplication.run(SloApplication.class, args);
    }
}
