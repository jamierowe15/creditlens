package com.creditlens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 * (scans com.creditlens and every sub-package for @Component, @Service, @RestController ...).
 */
@SpringBootApplication
public class CreditLensApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreditLensApplication.class, args);
    }
}
