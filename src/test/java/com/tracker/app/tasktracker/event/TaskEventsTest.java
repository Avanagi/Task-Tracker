package com.tracker.app.tasktracker.event;

import com.tracker.app.tasktracker.model.entity.tasks.SimpleTask;
import com.tracker.app.tasktracker.model.entity.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskEventsTest {

    private Object source;
    private SimpleTask task;
    private User user;

    @BeforeEach
    void setUp() {
        source = new Object();

        task = new SimpleTask();
        task.setId(1L);
        task.setTitle("Test Event Task");

        user = User.builder().id(1L).username("TestUser").build();
    }


    @Test
    void taskAssigneesChangedEvent_Positive_Success() {
        TaskAssigneesChangedEvent event = new TaskAssigneesChangedEvent(source, task, user, "ASSIGNED");

        assertEquals(source, event.getSource());
        assertEquals(task, event.getTask());
        assertEquals(user, event.getChangedUser());
        assertEquals("ASSIGNED", event.getAction());
    }

    @Test
    void taskAssigneesChangedEvent_Negative_NullSource() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new TaskAssigneesChangedEvent(null, task, user, "ASSIGNED");
        });
        assertEquals("null source", exception.getMessage());
    }

    @Test
    void taskAssigneesChangedEvent_Boundary_NullCustomFields() {
        TaskAssigneesChangedEvent event = new TaskAssigneesChangedEvent(source, null, null, null);
        assertNull(event.getTask());
        assertNull(event.getChangedUser());
        assertNull(event.getAction());
    }

    @Test
    void taskAssigneesChangedEvent_Boundary_EmptyActionString() {
        TaskAssigneesChangedEvent event = new TaskAssigneesChangedEvent(source, task, user, "");
        assertEquals("", event.getAction());
    }

    @Test
    void taskCreatedEvent_Positive_Success() {
        TaskCreatedEvent event = new TaskCreatedEvent(source, task);
        assertEquals(task, event.getTask());
    }

    @Test
    void taskCreatedEvent_Negative_NullTaskThrowsNPE() {
        assertThrows(NullPointerException.class, () -> {
            new TaskCreatedEvent(source, null);
        });
    }

    @Test
    void taskCreatedEvent_Boundary_NullTitle() {
        task.setTitle(null);
        TaskCreatedEvent event = new TaskCreatedEvent(source, task);
        assertNull(event.getTask().getTitle());
    }

    @Test
    void taskCreatedEvent_Boundary_NullSource() {
        assertThrows(IllegalArgumentException.class, () -> new TaskCreatedEvent(null, task));
    }

    @Test
    void taskDeletedEvent_Positive_Success() {
        TaskDeletedEvent event = new TaskDeletedEvent(source, 10L);
        assertEquals(10L, event.getTaskId());
    }

    @Test
    void taskDeletedEvent_Negative_NullSource() {
        assertThrows(IllegalArgumentException.class, () -> new TaskDeletedEvent(null, 10L));
    }

    @Test
    void taskDeletedEvent_Boundary_NullTaskId() {
        TaskDeletedEvent event = new TaskDeletedEvent(source, null);
        assertNull(event.getTaskId());
    }

    @Test
    void taskDeletedEvent_Boundary_NegativeTaskId() {
        TaskDeletedEvent event = new TaskDeletedEvent(source, -5L);
        assertEquals(-5L, event.getTaskId());
    }

    @Test
    void taskStatusChangedEvent_Positive_Success() {
        TaskStatusChangedEvent event = new TaskStatusChangedEvent(source, task);
        assertEquals(task, event.getTask());
    }

    @Test
    void taskStatusChangedEvent_Negative_NullTaskThrowsNPE() {
        assertThrows(NullPointerException.class, () -> {
            new TaskStatusChangedEvent(source, null);
        });
    }

    @Test
    void taskStatusChangedEvent_Boundary_NullTitle() {
        task.setTitle(null);
        TaskStatusChangedEvent event = new TaskStatusChangedEvent(source, task);
        assertNull(event.getTask().getTitle());
    }

    @Test
    void taskStatusChangedEvent_Boundary_NullSource() {
        assertThrows(IllegalArgumentException.class, () -> new TaskStatusChangedEvent(null, task));
    }
}