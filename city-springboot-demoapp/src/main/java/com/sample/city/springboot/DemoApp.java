package com.sample.city.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import reactor.core.publisher.Hooks;

@SpringBootApplication(scanBasePackages = {"com.sample.city.springboot"})
public class DemoApp {
    public static void main(String[] args) {
        Hooks.onOperatorDebug();
        SpringApplication.run(DemoApp.class, args);
    }
}
