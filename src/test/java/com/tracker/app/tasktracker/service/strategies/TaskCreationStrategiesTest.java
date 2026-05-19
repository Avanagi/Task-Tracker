package com.tracker.app.tasktracker.service.strategies;

import com.tracker.app.tasktracker.dto.TaskCreateDto;
import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import com.tracker.app.tasktracker.model.entity.tasks.BugTask;
import com.tracker.app.tasktracker.model.entity.tasks.EpicTask;
import com.tracker.app.tasktracker.model.entity.tasks.SimpleTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskCreationStrategiesTest {

    private BugCreationStrategy bugStrategy;
    private EpicCreationStrategy epicStrategy;
    private SimpleCreationStrategy simpleStrategy;

    private TaskCreateDto dto;

    @BeforeEach
    void setUp() {
        bugStrategy = new BugCreationStrategy();
        epicStrategy = new EpicCreationStrategy();
        simpleStrategy = new SimpleCreationStrategy();

        dto = new TaskCreateDto();
    }

    @Test
    void bugStrategy_Positive_CreatesBugTask() {
        dto.setStepsToReproduce("Step 1, Step 2");

        AbstractTask task = bugStrategy.create(dto);

        assertInstanceOf(BugTask.class, task);
        assertEquals("BUG", bugStrategy.getType());
        assertEquals("Step 1, Step 2", ((BugTask) task).getStepsToReproduce());
    }

    @Test
    void bugStrategy_Negative_NullDtoThrowsException() {
        assertThrows(NullPointerException.class, () -> bugStrategy.create(null));
    }

    @Test
    void bugStrategy_Boundary_NullStepsToReproduce() {
        dto.setStepsToReproduce(null);

        AbstractTask task = bugStrategy.create(dto);

        assertNotNull(task);
        assertNull(((BugTask) task).getStepsToReproduce());
    }

    @Test
    void bugStrategy_Boundary_EmptyStepsToReproduce() {
        dto.setStepsToReproduce("");

        AbstractTask task = bugStrategy.create(dto);

        assertNotNull(task);
        assertEquals("", ((BugTask) task).getStepsToReproduce());
    }

    @Test
    void epicStrategy_Positive_CreatesEpicTaskWithSubtasks() {
        dto.setSubtaskTitles(List.of("Subtask 1", "Subtask 2"));

        AbstractTask task = epicStrategy.create(dto);

        assertInstanceOf(EpicTask.class, task);
        assertEquals("EPIC", epicStrategy.getType());

        EpicTask epic = (EpicTask) task;
        assertEquals(2, epic.getSubtasks().size());
        assertEquals("Subtask 1", epic.getSubtasks().getFirst().getTitle());
        assertFalse(epic.getSubtasks().getFirst().isCompleted());
    }

    @Test
    void epicStrategy_Negative_NullDtoThrowsException() {
        assertThrows(NullPointerException.class, () -> epicStrategy.create(null));
    }

    @Test
    void epicStrategy_Boundary_NullSubtaskList() {
        dto.setSubtaskTitles(null);

        AbstractTask task = epicStrategy.create(dto);

        assertNotNull(task);
        assertTrue(((EpicTask) task).getSubtasks().isEmpty());
    }

    @Test
    void epicStrategy_Boundary_EmptySubtaskList() {
        dto.setSubtaskTitles(Collections.emptyList());

        AbstractTask task = epicStrategy.create(dto);

        assertNotNull(task);
        assertTrue(((EpicTask) task).getSubtasks().isEmpty());
    }

    @Test
    void simpleStrategy_Positive_CreatesSimpleTask() {
        AbstractTask task = simpleStrategy.create(dto);

        assertInstanceOf(SimpleTask.class, task);
        assertEquals("TASK", simpleStrategy.getType());
    }

    @Test
    void simpleStrategy_Negative_TypeMismatch() {
        assertNotEquals("BUG", simpleStrategy.getType());
        assertNotEquals("EPIC", simpleStrategy.getType());
    }

    @Test
    void simpleStrategy_Boundary_IgnoresNullDto() {
        AbstractTask task = simpleStrategy.create(null);

        assertNotNull(task);
        assertInstanceOf(SimpleTask.class, task);
    }

    @Test
    void simpleStrategy_Boundary_IgnoresExtraData() {
        dto.setStepsToReproduce("Some steps");
        dto.setSubtaskTitles(List.of("Some subtask"));

        AbstractTask task = simpleStrategy.create(dto);

        assertInstanceOf(SimpleTask.class, task);
    }
}