package com.aether;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AetherApplication {

    public static void main(String[] args) {
        SpringApplication.run(AetherApplication.class, args);
    }
}
