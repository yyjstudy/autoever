package com.autoever.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AutoeverMemberSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoeverMemberSystemApplication.class, args);
    }

}