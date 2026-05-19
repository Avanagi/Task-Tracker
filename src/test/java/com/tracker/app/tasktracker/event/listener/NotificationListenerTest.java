package com.tracker.app.tasktracker.event.listener;

import com.tracker.app.tasktracker.event.TaskAssigneesChangedEvent;
import com.tracker.app.tasktracker.event.TaskCreatedEvent;
import com.tracker.app.tasktracker.event.TaskDeletedEvent;
import com.tracker.app.tasktracker.event.TaskStatusChangedEvent;
import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import com.tracker.app.tasktracker.model.entity.tasks.SimpleTask;
import com.tracker.app.tasktracker.model.entity.users.User;
import com.tracker.app.tasktracker.model.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationListener notificationListener;

    private User assignee;
    private AbstractTask task;

    @BeforeEach
    void setUp() {
        User reporter = User.builder().id(1L).username("Reporter").email("rep@test.com").build();
        assignee = User.builder().id(2L).username("Assignee").email("assig@test.com").build();

        task = new SimpleTask();
        task.setId(10L);
        task.setTitle("Test Task");
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setReporter(reporter);
        task.setAssignees(Set.of(assignee));
    }

    @Test
    void handleTaskStatusChange_Positive_Success() {
        TaskStatusChangedEvent event = new TaskStatusChangedEvent(this, task);

        notificationListener.handleTaskStatusChange(event);

        verify(messagingTemplate, times(1)).convertAndSendToUser(eq("Reporter"), anyString(), anyString());
        verify(messagingTemplate, times(1)).convertAndSendToUser(eq("Assignee"), anyString(), anyString());
        verify(messagingTemplate, times(1)).convertAndSend("/topic/tasks", "UPDATE");
    }

    @Test
    void handleTaskStatusChange_Negative_NullTask() {
        assertThrows(NullPointerException.class, () -> new TaskStatusChangedEvent(this, null));
    }

    @Test
    void handleTaskStatusChange_Boundary_NullReporterAndAssignees() {
        task.setReporter(null);
        task.setAssignees(null);
        TaskStatusChangedEvent event = new TaskStatusChangedEvent(this, task);

        notificationListener.handleTaskStatusChange(event);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), anyString());
        verify(messagingTemplate, times(1)).convertAndSend("/topic/tasks", "UPDATE");
    }

    @Test
    void handleTaskStatusChange_Boundary_EmptyAssignees() {
        task.setAssignees(Collections.emptySet());
        TaskStatusChangedEvent event = new TaskStatusChangedEvent(this, task);

        notificationListener.handleTaskStatusChange(event);

        verify(messagingTemplate, times(1)).convertAndSendToUser(eq("Reporter"), anyString(), anyString());
        verify(messagingTemplate, never()).convertAndSendToUser(eq("Assignee"), anyString(), anyString());
    }

    @Test
    void handleTaskCreated_Positive_Success() {
        TaskCreatedEvent event = new TaskCreatedEvent(this, task);

        notificationListener.handleTaskCreated(event);

        verify(messagingTemplate, times(1)).convertAndSend("/topic/tasks", "UPDATE");
    }

    @Test
    void handleTaskCreated_Negative_AssigneeWithoutEmail() {
        assignee.setEmail(null);
        task.setAssignees(Set.of(assignee));
        TaskCreatedEvent event = new TaskCreatedEvent(this, task);

        notificationListener.handleTaskCreated(event);

        verify(messagingTemplate, times(1)).convertAndSend("/topic/tasks", "UPDATE");
    }

    @Test
    void handleTaskCreated_Boundary_NullAssignees() {
        task.setAssignees(null);
        TaskCreatedEvent event = new TaskCreatedEvent(this, task);

        notificationListener.handleTaskCreated(event);

        verify(messagingTemplate, times(1)).convertAndSend("/topic/tasks", "UPDATE");
    }

    @Test
    void handleTaskCreated_Boundary_EmptyAssignees() {
        task.setAssignees(Collections.emptySet());
        TaskCreatedEvent event = new TaskCreatedEvent(this, task);

        notificationListener.handleTaskCreated(event);

        verify(messagingTemplate, times(1)).convertAndSend("/topic/tasks", "UPDATE");
    }

    @Test
    void handleTaskDeleted_Positive_Success() {
        TaskDeletedEvent event = new TaskDeletedEvent(this, 10L);

        notificationListener.handleTaskDeleted(event);

        verify(messagingTemplate, times(1)).convertAndSend("/topic/tasks", "UPDATE");
    }

    @Test
    void handleTaskDeleted_Negative_NullId() {
        TaskDeletedEvent event = new TaskDeletedEvent(this, null);

        notificationListener.handleTaskDeleted(event);

        verify(messagingTemplate, times(1)).convertAndSend("/topic/tasks", "UPDATE");
    }

    @Test
    void handleTaskDeleted_Boundary_MaxLongId() {
        TaskDeletedEvent event = new TaskDeletedEvent(this, Long.MAX_VALUE);
        notificationListener.handleTaskDeleted(event);
        verify(messagingTemplate, times(1)).convertAndSend("/topic/tasks", "UPDATE");
    }

    @Test
    void handleTaskDeleted_Boundary_NegativeId() {
        TaskDeletedEvent event = new TaskDeletedEvent(this, -1L);
        notificationListener.handleTaskDeleted(event);
        verify(messagingTemplate, times(1)).convertAndSend("/topic/tasks", "UPDATE");
    }

    @Test
    void handleAssigneesChanged_Positive_Assigned() {
        TaskAssigneesChangedEvent event = new TaskAssigneesChangedEvent(this, task, assignee, "ASSIGNED");

        notificationListener.handleAssigneesChanged(event);

        verify(messagingTemplate, times(1)).convertAndSendToUser(eq("Assignee"), eq("/queue/notifications"), anyString());
        verify(messagingTemplate, times(1)).convertAndSend("/topic/tasks", "UPDATE");
    }

    @Test
    void handleAssigneesChanged_Negative_NullAction() {
        TaskAssigneesChangedEvent event = new TaskAssigneesChangedEvent(this, task, assignee, null);

        assertThrows(NullPointerException.class, () -> notificationListener.handleAssigneesChanged(event));
    }

    @Test
    void handleAssigneesChanged_Boundary_Removed() {
        TaskAssigneesChangedEvent event = new TaskAssigneesChangedEvent(this, task, assignee, "REMOVED");

        notificationListener.handleAssigneesChanged(event);

        verify(messagingTemplate, times(1)).convertAndSendToUser(eq("Assignee"), eq("/queue/notifications"), anyString());
    }

    @Test
    void handleAssigneesChanged_Boundary_UnknownAction() {
        TaskAssigneesChangedEvent event = new TaskAssigneesChangedEvent(this, task, assignee, "UNKNOWN_ACTION");

        notificationListener.handleAssigneesChanged(event);

        verify(messagingTemplate, times(1)).convertAndSendToUser(eq("Assignee"), anyString(), anyString());
    }
}