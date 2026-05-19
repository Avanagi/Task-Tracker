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

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@DataJpaTest
class EpicCompositionIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;

    private User reporter;

    @BeforeEach
    void setUp() {
        log.info("Setting up test data...");
        taskRepository.deleteAll();
        userRepository.deleteAll();

        reporter = User.builder()
                .username("ReporterUser")
                .email("rep@test.com")
                .password("StrongPass123!")
                .build();
        reporter = userRepository.saveAndFlush(reporter);
        log.info("Saved reporter with ID: {}", reporter.getId());
    }

    @Test
    void composition_Positive_SaveEpicPersistsSubtasks() {
        log.info("Running composition_Positive_SaveEpicPersistsSubtasks");
        EpicTask epic = new EpicTask();
        epic.setTitle("EpicTitle");
        epic.setStatus(TaskStatus.TODO);
        epic.setDueDate(LocalDateTime.now().plusDays(10));
        epic.setReporter(reporter);

        epic.getSubtasks().add(new Subtask(null, "SubtaskTitleOne", false));

        log.info("Persisting Epic with 1 subtask...");
        EpicTask saved = taskRepository.saveAndFlush(epic);
        entityManager.clear();

        EpicTask loaded = (EpicTask) taskRepository.findById(saved.getId()).orElseThrow();
        log.info("Checking loaded Epic subtasks count: {}", loaded.getSubtasks().size());
        assertEquals(1, loaded.getSubtasks().size());
        log.info("Successfully verified cascade save.");
    }

    @Test
    void composition_Negative_InvalidSubtaskThrowsException() {
        log.info("Running composition_Negative_InvalidSubtaskThrowsException");
        EpicTask epic = new EpicTask();
        epic.setTitle("EpicTitle");
        epic.setStatus(TaskStatus.TODO);
        epic.setDueDate(LocalDateTime.now().plusDays(10));
        epic.setReporter(reporter);

        log.info("Adding invalid subtask (title too short)...");
        epic.getSubtasks().add(new Subtask(null, "Ab", false));

        log.info("Attempting to save Epic with invalid data...");
        assertThrows(ConstraintViolationException.class, () -> {
            taskRepository.saveAndFlush(epic);
        });
        log.info("Caught expected Exception.");
    }

    @Test
    void composition_Boundary_RemoveSubtaskKeepsEpic() {
        log.info("Starting test: composition_Boundary_RemoveSubtaskKeepsEpic");

        EpicTask epic = new EpicTask();
        epic.setTitle("Integrity Test Epic");
        epic.setStatus(TaskStatus.TODO);
        epic.setDueDate(LocalDateTime.now().plusDays(10));
        epic.setReporter(reporter);

        epic.getSubtasks().add(new Subtask(null, "Persistent subtask", false));
        epic.getSubtasks().add(new Subtask(null, "Transient subtask", false));

        log.info("Persisting EpicTask with two subtasks...");
        EpicTask saved = taskRepository.saveAndFlush(epic);
        Long epicId = saved.getId();
        Long subIdToDelete = saved.getSubtasks().get(1).getId();

        log.info("Epic saved with ID: {}. Target subtask for removal ID: {}", epicId, subIdToDelete);

        log.info("Removing subtask from collection and synchronizing with database...");
        saved.getSubtasks().remove(1);
        taskRepository.saveAndFlush(saved);
        entityManager.clear();

        log.info("Checking entities existence in database...");
        Subtask deletedSub = entityManager.find(Subtask.class, subIdToDelete);
        EpicTask remainingEpic = entityManager.find(EpicTask.class, epicId);

        log.info("Search result - Deleted Subtask: {}, Parent Epic: {}", deletedSub, remainingEpic);

        assertNull(deletedSub);
        assertNotNull(remainingEpic);
        assertEquals(1, remainingEpic.getSubtasks().size());

        log.info("Successfully verified: orphan subtask removed, but parent epic remains intact.");
    }

    @Test
    void composition_Boundary_DeleteEpicRemovesAllSubtasks() {
        log.info("Running composition_Boundary_DeleteEpicRemovesAllSubtasks");
        EpicTask epic = new EpicTask();
        epic.setTitle("EpicToDelete");
        epic.setStatus(TaskStatus.TODO);
        epic.setDueDate(LocalDateTime.now().plusDays(10));
        epic.setReporter(reporter);
        epic.getSubtasks().add(new Subtask(null, "Subtask1", false));

        EpicTask saved = taskRepository.saveAndFlush(epic);
        Long subId = saved.getSubtasks().getFirst().getId();
        log.info("Epic saved with subtask ID: {}", subId);

        log.info("Deleting Epic with ID: {}", saved.getId());
        taskRepository.delete(saved);
        taskRepository.flush();
        entityManager.clear();

        Subtask orphanedSubtask = entityManager.find(Subtask.class, subId);
        log.info("Search result for orphaned subtask: {}", orphanedSubtask);
        assertNull(orphanedSubtask);
        log.info("Successfully verified cascade delete.");
    }

    @Test
    void composition_Boundary_MaxSubtasksLimit() {
        log.info("Running composition_Boundary_MaxSubtasksLimit");
        EpicTask epic = new EpicTask();
        epic.setTitle("MaxSubtasksEpic");
        epic.setStatus(TaskStatus.TODO);
        epic.setDueDate(LocalDateTime.now().plusDays(10));
        epic.setReporter(reporter);

        log.info("Filling epic with 50 subtasks (boundary value)...");
        for (int i = 0; i < 50; i++) {
            epic.getSubtasks().add(new Subtask(null, "Subtask title " + i, false));
        }

        assertDoesNotThrow(() -> taskRepository.saveAndFlush(epic));
        log.info("Successfully saved epic with 50 subtasks.");

        log.info("Adding 51st subtask (exceeding limit)...");
        epic.getSubtasks().add(new Subtask(null, "One too many", false));

        assertThrows(ConstraintViolationException.class, () -> {
            taskRepository.saveAndFlush(epic);
        });
        log.info("Caught expected exception for exceeding collection size limit.");
    }

    @Test
    void composition_Negative_SubtaskCannotExistWithoutEpic() {
        log.info("Starting test: composition_Negative_SubtaskCannotExistWithoutEpic");
        Subtask lonelySubtask = new Subtask(null, "Lonely Subtask Title", false);

        log.info("Attempting to persist subtask without parent epic association...");
        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(lonelySubtask);
        });
        log.info("Integrity check passed: standalone subtask persistence rejected.");
    }
}