package com.tracker.app.tasktracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskTrackerApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void mainMethod_Positive_StartsApplication() {
        String[] args = {
                "--server.port=0",
        };

        assertDoesNotThrow(() -> TaskTrackerApplication.main(args));
    }
}