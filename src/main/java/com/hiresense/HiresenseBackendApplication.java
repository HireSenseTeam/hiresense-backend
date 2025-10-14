package com.hiresense;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class HiresenseBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HiresenseBackendApplication.class, args);
    }
}
