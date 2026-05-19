package com.tracker.app.tasktracker.model.entity.tasks;

import com.tracker.app.tasktracker.event.TaskStatusChangedEvent;
import com.tracker.app.tasktracker.model.interfaces.Assignable;
import com.tracker.app.tasktracker.model.interfaces.Auditable;
import com.tracker.app.tasktracker.model.interfaces.TaskTypeable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectureIntegrationTest {

    @Test
    void generalization_Positive_Inherit() {
        BugTask bug = new BugTask();
        bug.setTitle("Title");
        assertInstanceOf(AbstractTask.class, bug);
    }

    @Test
    void generalization_Negative_TypeMismatch() {
        AbstractTask epic = new EpicTask();
        assertThrows(ClassCastException.class, () -> {
            BugTask b = (BugTask) epic;
        });
    }

    @Test
    void generalization_Boundary_ParentMethod() {
        SimpleTask task = new SimpleTask();
        task.setTitle("Test");
        assertEquals("Test", task.getTitle());
    }

    @Test
    void generalization_Boundary_GetType() {
        AbstractTask bug = new BugTask();
        assertEquals("BUG", bug.getType());
    }

    @Test
    void realization_Positive_ImplementsAuditable() {
        EpicTask epic = new EpicTask();
        assertInstanceOf(Auditable.class, epic);
    }

    @Test
    void realization_Negative_CheckWrongInterface() {
        assertFalse(this instanceof Auditable);
    }

    @Test
    void realization_Boundary_MultipleInterfaces() {
        SimpleTask task = new SimpleTask();
        assertInstanceOf(Assignable.class, task);
        assertInstanceOf(Auditable.class, task);
    }

    @Test
    void realization_Boundary_InvokeInterfaceMethod() {
        TaskTypeable task = new SimpleTask();
        assertEquals("TASK", task.getType());
    }

    @Test
    void dependency_Positive_EventTrigger() {
        TaskStatusChangedEvent event = new TaskStatusChangedEvent(this, new SimpleTask());
        assertNotNull(event.getTask());
    }

    @Test
    void dependency_Negative_NullEventData() {
        assertThrows(NullPointerException.class, () -> new TaskStatusChangedEvent(this, null));
    }

    @Test
    void dependency_Boundary_EmptyTask() {
        SimpleTask emptyTask = new SimpleTask();
        TaskStatusChangedEvent event = new TaskStatusChangedEvent(this, emptyTask);
        assertEquals(emptyTask, event.getTask());
    }

    @Test
    void dependency_Boundary_LongTitle() {
        SimpleTask task = new SimpleTask();
        task.setTitle("A".repeat(255));
        TaskStatusChangedEvent event = new TaskStatusChangedEvent(this, task);
        assertEquals(255, event.getTask().getTitle().length());
    }
}