package com.springanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpringAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAnalyzerApplication.class, args);
    }
}
