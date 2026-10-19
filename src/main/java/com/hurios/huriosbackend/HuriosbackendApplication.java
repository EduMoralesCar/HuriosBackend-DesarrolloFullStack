package com.hurios.huriosbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.hurios.huriosbackend")
public class HuriosbackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HuriosbackendApplication.class, args);
    }
}