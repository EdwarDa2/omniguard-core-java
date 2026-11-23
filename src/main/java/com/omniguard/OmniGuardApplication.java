package com.omniguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.omniguard")
@EntityScan(basePackages = "com.omniguard.adapter.out.persistence.entity")
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class OmniGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(OmniGuardApplication.class, args);
    }
}