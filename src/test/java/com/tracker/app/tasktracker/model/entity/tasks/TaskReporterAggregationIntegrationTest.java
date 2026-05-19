package com.tracker.app.tasktracker.model.entity.tasks;

import com.tracker.app.tasktracker.model.entity.users.User;
import com.tracker.app.tasktracker.model.enums.TaskStatus;
import com.tracker.app.tasktracker.repository.TaskRepository;
import com.tracker.app.tasktracker.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@DataJpaTest
class TaskReporterAggregationIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;

    private User sharedReporter;

    @BeforeEach
    void setUp() {
        log.info("Cleaning database and creating a reporter for aggregation tests...");
        taskRepository.deleteAll();
        userRepository.deleteAll();

        sharedReporter = User.builder()
                .username("AggregationMaster")
                .email("agg@test.com")
                .password("StrongPass123!")
                .build();
        sharedReporter = entityManager.persistAndFlush(sharedReporter);
    }

    @Test
    void aggregation_Positive_LinkTaskWithReporter() {
        log.info("Starting test: aggregation_Positive_LinkTaskWithReporter");
        SimpleTask task = new SimpleTask();
        task.setTitle("Standard Task");
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(LocalDateTime.now().plusDays(5));
        task.setReporter(sharedReporter);

        log.info("Persisting task linked to reporter ID: {}", sharedReporter.getId());
        SimpleTask saved = taskRepository.saveAndFlush(task);
        entityManager.clear();

        SimpleTask loaded = (SimpleTask) taskRepository.findById(saved.getId()).orElseThrow();
        assertNotNull(loaded.getReporter());
        assertEquals(sharedReporter.getUsername(), loaded.getReporter().getUsername());
        log.info("Aggregation link successfully verified.");
    }

    @Test
    void aggregation_Negative_SaveTaskWithoutReporterFails() {
        log.info("Starting test: aggregation_Negative_SaveTaskWithoutReporterFails");
        SimpleTask lonelyTask = new SimpleTask();
        lonelyTask.setTitle("Reporterless Task");
        lonelyTask.setStatus(TaskStatus.TODO);
        lonelyTask.setDueDate(LocalDateTime.now().plusDays(5));

        log.info("Attempting to save task without mandatory reporter association...");
        assertThrows(Exception.class, () -> {
            taskRepository.saveAndFlush(lonelyTask);
        });
        log.info("Result: database correctly rejected task without an aggregator.");
    }

    @Test
    void aggregation_Boundary_DeleteTaskDoesNotAffectReporter() {
        log.info("Starting test: aggregation_Boundary_DeleteTaskDoesNotAffectReporter");
        SimpleTask task = new SimpleTask();
        task.setTitle("Ephemeral Task");
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(LocalDateTime.now().plusDays(5));
        task.setReporter(sharedReporter);

        SimpleTask saved = taskRepository.saveAndFlush(task);
        Long taskId = saved.getId();
        Long reporterId = sharedReporter.getId();

        log.info("Deleting task ID: {}, expecting reporter ID: {} to survive", taskId, reporterId);
        taskRepository.delete(saved);
        taskRepository.flush();
        entityManager.clear();

        assertFalse(taskRepository.existsById(taskId));
        User reporterStillExists = entityManager.find(User.class, reporterId);

        assertNotNull(reporterStillExists, "Reporter must remain in DB after task deletion");
        log.info("Boundary check passed: task lifecycle is independent of reporter's existence.");
    }

    @Test
    void aggregation_Boundary_MultipleTasksSingleReporter() {
        log.info("Starting test: aggregation_Boundary_MultipleTasksSingleReporter");

        SimpleTask task1 = new SimpleTask();
        task1.setTitle("Task One");
        task1.setStatus(TaskStatus.TODO);
        task1.setDueDate(LocalDateTime.now().plusDays(5));
        task1.setReporter(sharedReporter);

        SimpleTask task2 = new SimpleTask();
        task2.setTitle("Task Two");
        task2.setStatus(TaskStatus.TODO);
        task2.setDueDate(LocalDateTime.now().plusDays(5));
        task2.setReporter(sharedReporter);

        log.info("Assigning two different tasks to the same reporter...");
        taskRepository.saveAndFlush(task1);
        taskRepository.saveAndFlush(task2);
        entityManager.clear();

        long taskCount = taskRepository.findAll().stream()
                .filter(t -> t.getReporter().getId().equals(sharedReporter.getId()))
                .count();

        log.info("Reporter ID: {} is aggregating {} tasks", sharedReporter.getId(), taskCount);
        assertEquals(2, taskCount, "One reporter should be able to aggregate multiple tasks");
    }
}