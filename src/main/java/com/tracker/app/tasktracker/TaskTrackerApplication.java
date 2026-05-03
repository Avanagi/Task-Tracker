package com.tracker.app.tasktracker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class TaskTrackerApplication {

    static void main(String[] args) {
        log.info("Starting Task Tracker");
        SpringApplication.run(TaskTrackerApplication.class, args);
    }

}
