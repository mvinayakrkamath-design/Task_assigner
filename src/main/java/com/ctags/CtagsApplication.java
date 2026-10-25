package com.ctags;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Corporate Task Assignment and Grading System - Phase 1. */
@SpringBootApplication
@EnableScheduling
public class CtagsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CtagsApplication.class, args);
    }
}
