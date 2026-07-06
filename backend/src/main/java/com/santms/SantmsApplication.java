package com.santms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SANTMS - Secure Automatic Network Topology & Management System
 * Main Application Entry Point
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class SantmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SantmsApplication.class, args);
        System.out.println("""
            ╔══════════════════════════════════════════════════════════╗
            ║   SANTMS - Network Management System                    ║
            ║   Version 1.0.0  |  Running on http://localhost:8080    ║
            ║   Status: ACTIVE  |  Environment: Development           ║
            ╚══════════════════════════════════════════════════════════╝
            """);
    }
}
