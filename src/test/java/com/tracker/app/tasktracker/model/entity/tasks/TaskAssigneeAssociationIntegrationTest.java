package com.tracker.app.tasktracker.model.entity.tasks;

import com.tracker.app.tasktracker.model.entity.users.User;
import com.tracker.app.tasktracker.model.enums.TaskStatus;
import com.tracker.app.tasktracker.repository.TaskRepository;
import com.tracker.app.tasktracker.repository.UserRepository;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@DataJpaTest
class TaskAssigneeAssociationIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;

    private User reporter;
    private User worker1;
    private User worker2;

    @BeforeEach
    void setUp() {
        log.info("Setting up clean database state for association testing...");
        taskRepository.deleteAll();
        userRepository.deleteAll();

        reporter = userRepository.saveAndFlush(User.builder()
                .username("ReporterUser").email("rep@test.com").password("StrongPass123!").build());
        worker1 = userRepository.saveAndFlush(User.builder()
                .username("WorkerOne").email("w1@test.com").password("StrongPass123!").build());
        worker2 = userRepository.saveAndFlush(User.builder()
                .username("WorkerTwo").email("w2@test.com").password("StrongPass123!").build());

        log.info("Users initialized. Reporter ID: {}, Workers: {}, {}", reporter.getId(), worker1.getId(), worker2.getId());
    }

    @Test
    void association_Positive_LinkTaskWithMultipleAssignees() {
        log.info("Starting test: association_Positive_LinkTaskWithMultipleAssignees");
        SimpleTask task = new SimpleTask();
        task.setTitle("Collaborative Task");
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(LocalDateTime.now().plusDays(3));
        task.setReporter(reporter);

        Set<User> assignees = new HashSet<>();
        assignees.add(worker1);
        assignees.add(worker2);
        task.setAssignees(assignees);

        log.info("Saving task with two assignees...");
        SimpleTask saved = taskRepository.saveAndFlush(task);
        entityManager.clear();

        SimpleTask loaded = (SimpleTask) taskRepository.findById(saved.getId()).orElseThrow();
        log.info("Loaded task ID: {} with assignee count: {}", loaded.getId(), loaded.getAssignees().size());

        assertEquals(2, loaded.getAssignees().size());
        assertTrue(loaded.getAssignees().stream().anyMatch(u -> u.getUsername().equals("WorkerOne")));
        assertTrue(loaded.getAssignees().stream().anyMatch(u -> u.getUsername().equals("WorkerTwo")));
    }

    @Test
    void association_Negative_AssignUnsavedUserThrowsException() {
        log.info("Starting test: association_Negative_AssignUnsavedUserThrowsException");
        SimpleTask task = new SimpleTask();
        task.setTitle("Invalid Assignment Task");
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(LocalDateTime.now().plusDays(3));
        task.setReporter(reporter);

        User transientUser = User.builder()
                .username("Ghost").email("ghost@test.com").password("StrongPass123!").build();

        task.getAssignees().add(transientUser);

        log.info("Attempting to save task with transient user (not in DB)...");
        assertThrows(Exception.class, () -> {
            taskRepository.saveAndFlush(task);
        });
        log.info("Caught expected exception: Association with unsaved entity rejected.");
    }

    @Test
    void association_Boundary_RemoveAssigneeIndependently() {
        log.info("Running: association_Boundary_RemoveAssigneeIndependently");

        SimpleTask task = new SimpleTask();
        task.setTitle("Link Test");
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(LocalDateTime.now().plusDays(5));
        task.setReporter(reporter);
        task.getAssignees().add(worker1);

        SimpleTask saved = taskRepository.saveAndFlush(task);
        entityManager.flush();
        entityManager.clear();

        log.info("Removing user from task assignees using removeIf by ID...");

        SimpleTask taskToUpdate = (SimpleTask) taskRepository.findById(saved.getId()).orElseThrow();

        Long idToRemove = worker1.getId();
        taskToUpdate.getAssignees().removeIf(u -> u.getId().equals(idToRemove));

        taskRepository.saveAndFlush(taskToUpdate);
        entityManager.flush();
        entityManager.clear();

        SimpleTask loaded = (SimpleTask) taskRepository.findById(saved.getId()).orElseThrow();
        User userInDb = userRepository.findById(idToRemove).orElse(null);

        log.info("Checking results: task assignees count = {}, worker in DB exists = {}",
                loaded.getAssignees().size(), (userInDb != null));

        assertEquals(0, loaded.getAssignees().size(), "Task should have 0 assignees after removal");
        assertNotNull(userInDb, "User must still exist in the database");
    }

    @Test
    void association_Boundary_MaxAssigneesLimit() {
        log.info("Starting test: association_Boundary_MaxAssigneesLimit");
        SimpleTask task = new SimpleTask();
        task.setTitle("Scale Test Task");
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(LocalDateTime.now().plusDays(3));
        task.setReporter(reporter);

        log.info("Filling task with 20 unique users (boundary value)...");
        for (int i = 0; i < 20; i++) {
            User u = userRepository.save(User.builder()
                    .username("BatchUser" + i).email("user" + i + "@test.com").password("StrongPass123!").build());
            task.getAssignees().add(u);
        }
        taskRepository.saveAndFlush(task);
        log.info("Successfully saved task with 20 assignees.");

        log.info("Adding 21st user to exceed limit...");
        User extraUser = userRepository.save(User.builder()
                .username("ExtraUser").email("extra@test.com").password("StrongPass123!").build());
        task.getAssignees().add(extraUser);

        assertThrows(ConstraintViolationException.class, () -> {
            taskRepository.saveAndFlush(task);
        });
        log.info("Validation correctly blocked assignment exceeding the 20-user limit.");
    }
}