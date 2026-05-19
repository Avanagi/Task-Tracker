package com.tracker.app.tasktracker.model.entity.tasks;

import com.tracker.app.tasktracker.model.entity.users.User;
import com.tracker.app.tasktracker.model.enums.TaskStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class TaskEntitiesTest {

    private static Validator validator;
    private User validReporter;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setUp() {
        validReporter = User.builder().id(1L).username("Reporter").email("test@test.com").password("Pass1!").build();
    }


    @Test
    void simpleTask_Positive_ValidEntity() {
        SimpleTask task = new SimpleTask();
        task.setTitle("Valid Title");
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(LocalDateTime.now().plusDays(1));
        task.setReporter(validReporter);

        Set<ConstraintViolation<SimpleTask>> violations = validator.validate(task);
        assertTrue(violations.isEmpty(), "Ожидалось отсутствие ошибок валидации");
        assertEquals("TASK", task.getType(), "Тип должен быть TASK");
    }

    @Test
    void simpleTask_Negative_MissingRequiredFields() {
        SimpleTask task = new SimpleTask();

        Set<ConstraintViolation<SimpleTask>> violations = validator.validate(task);
        assertFalse(violations.isEmpty());
        assertTrue(violations.size() >= 3);
    }

    @Test
    void simpleTask_Boundary_TitleLength() {
        SimpleTask task = new SimpleTask();
        task.setDueDate(LocalDateTime.now().plusDays(1));
        task.setReporter(validReporter);

        task.setTitle("Abc");
        assertTrue(validator.validate(task).isEmpty());

        task.setTitle("A".repeat(255));
        assertTrue(validator.validate(task).isEmpty());

        task.setTitle("Ab");
        assertFalse(validator.validate(task).isEmpty());
    }

    @Test
    void simpleTask_Boundary_DescriptionLength() {
        SimpleTask task = new SimpleTask();
        task.setTitle("Valid Title");
        task.setDueDate(LocalDateTime.now().plusDays(1));
        task.setReporter(validReporter);

        task.setDescription("D".repeat(2000));
        assertTrue(validator.validate(task).isEmpty());

        task.setDescription("D".repeat(2001));
        Set<ConstraintViolation<SimpleTask>> violations = validator.validate(task);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("exceed 2000 characters"));
    }

    @Test
    void bugTask_Positive_ValidEntity() {
        BugTask bug = new BugTask();
        bug.setTitle("Valid Bug");
        bug.setDueDate(LocalDateTime.now().plusDays(1));
        bug.setReporter(validReporter);
        bug.setStepsToReproduce("1. Click button. 2. See error.");

        Set<ConstraintViolation<BugTask>> violations = validator.validate(bug);
        assertTrue(violations.isEmpty());
        assertEquals("BUG", bug.getType());
    }

    @Test
    void bugTask_Negative_NullStepsToReproduce() {
        BugTask bug = new BugTask();
        bug.setTitle("Valid Bug");
        bug.setDueDate(LocalDateTime.now().plusDays(1));
        bug.setReporter(validReporter);
        bug.setStepsToReproduce(null);
        Set<ConstraintViolation<BugTask>> violations = validator.validate(bug);
        assertFalse(violations.isEmpty());
    }

    @Test
    void bugTask_Boundary_StepsMinLength() {
        BugTask bug = new BugTask();
        bug.setTitle("Valid Bug");
        bug.setDueDate(LocalDateTime.now().plusDays(1));
        bug.setReporter(validReporter);

        bug.setStepsToReproduce("1234567890");
        assertTrue(validator.validate(bug).isEmpty());

        bug.setStepsToReproduce("123456789");
        assertFalse(validator.validate(bug).isEmpty());
    }

    @Test
    void bugTask_Boundary_StepsMaxLength() {
        BugTask bug = new BugTask();
        bug.setTitle("Valid Bug");
        bug.setDueDate(LocalDateTime.now().plusDays(1));
        bug.setReporter(validReporter);

        bug.setStepsToReproduce("S".repeat(5000));
        assertTrue(validator.validate(bug).isEmpty());

        bug.setStepsToReproduce("S".repeat(5001));
        assertFalse(validator.validate(bug).isEmpty());
    }

    @Test
    void epicTask_Positive_ValidEntity() {
        EpicTask epic = new EpicTask();
        epic.setTitle("Valid Epic");
        epic.setDueDate(LocalDateTime.now().plusDays(1));
        epic.setReporter(validReporter);
        epic.setSubtasks(List.of(new Subtask(1L, "Sub 1", false)));

        Set<ConstraintViolation<EpicTask>> violations = validator.validate(epic);
        assertTrue(violations.isEmpty());
        assertEquals("EPIC", epic.getType());
    }

    @Test
    void epicTask_Negative_EmptySubtasks() {
        EpicTask epic = new EpicTask();
        epic.setTitle("Valid Epic");
        epic.setDueDate(LocalDateTime.now().plusDays(1));
        epic.setReporter(validReporter);
        epic.setSubtasks(Collections.emptyList()); // Ошибка (@NotEmpty)

        Set<ConstraintViolation<EpicTask>> violations = validator.validate(epic);
        assertFalse(violations.isEmpty());
        assertTrue(violations.iterator().next().getMessage().contains("least one subtask"));
    }

    @Test
    void epicTask_Boundary_MaxSubtasks() {
        EpicTask epic = new EpicTask();
        epic.setTitle("Valid Epic");
        epic.setDueDate(LocalDateTime.now().plusDays(1));
        epic.setReporter(validReporter);

        List<Subtask> maxSubtasks = IntStream.range(0, 50)
                .mapToObj(i -> new Subtask((long) i, "Sub " + i, false))
                .collect(Collectors.toList());
        epic.setSubtasks(maxSubtasks);

        assertTrue(validator.validate(epic).isEmpty());
    }

    @Test
    void epicTask_Boundary_ExceedMaxSubtasks() {
        EpicTask epic = new EpicTask();
        epic.setTitle("Valid Epic");
        epic.setDueDate(LocalDateTime.now().plusDays(1));
        epic.setReporter(validReporter);

        List<Subtask> tooManySubtasks = IntStream.range(0, 51)
                .mapToObj(i -> new Subtask((long) i, "Sub " + i, false))
                .collect(Collectors.toList());
        epic.setSubtasks(tooManySubtasks);

        Set<ConstraintViolation<EpicTask>> violations = validator.validate(epic);
        assertFalse(violations.isEmpty());
        assertTrue(violations.iterator().next().getMessage().contains("more than 50"));
    }

    @Test
    void prePersist_Positive_SetsCreatedAt() {
        SimpleTask task = new SimpleTask();
        assertNull(task.getCreatedAt());

        task.prePersist();

        assertNotNull(task.getCreatedAt());
        assertTrue(task.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    void equalsAndHashCode_Positive_EntityComparison() {
        SimpleTask task1 = new SimpleTask();
        task1.setId(10L);

        SimpleTask task2 = new SimpleTask();
        task2.setId(10L);

        SimpleTask task3 = new SimpleTask();
        task3.setId(20L);

        assertEquals(task1, task2);
        assertEquals(task1.hashCode(), task2.hashCode());

        assertNotEquals(task1, task3);
    }
}