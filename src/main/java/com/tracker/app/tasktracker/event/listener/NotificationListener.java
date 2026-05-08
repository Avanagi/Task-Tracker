package com.tracker.app.tasktracker.event.listener;

import com.tracker.app.tasktracker.event.TaskCreatedEvent;
import com.tracker.app.tasktracker.event.TaskDeletedEvent;
import com.tracker.app.tasktracker.event.TaskStatusChangedEvent;
import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import com.tracker.app.tasktracker.model.entity.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationListener {
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleTaskStatusChange(TaskStatusChangedEvent event) {
        AbstractTask task = event.getTask();
        Set<User> recipients = new HashSet<>();
        if (task.getReporter() != null) {
            recipients.add(task.getReporter());
        }
        if (task.getAssignees() != null) {
            recipients.addAll(task.getAssignees());
        }
        for (User user : recipients) {
            if (user.getEmail() != null) {
                log.info("Sending email on [{}]: Task's status '{}' changed to {}",
                        user.getEmail(), task.getTitle(), task.getStatus());
            }
        }
        messagingTemplate.convertAndSend("/topic/tasks", "UPDATE");
    }

    @EventListener
    public void handleTaskCreated(TaskCreatedEvent event) {
        AbstractTask task = event.getTask();
        if (task.getAssignees() != null && !task.getAssignees().isEmpty()) {
            for (User assignee : task.getAssignees()) {
                if (assignee.getEmail() != null) {
                    log.info("Sending email on [{}]: You are assigned to the new task '{}'",
                            assignee.getEmail(), task.getTitle());
                }
            }
        }
        messagingTemplate.convertAndSend("/topic/tasks", "UPDATE");
    }

    @EventListener
    public void handleTaskDeleted(TaskDeletedEvent event) {
        log.info("Task removed: ID {}. Updating clients", event.getTaskId());
        messagingTemplate.convertAndSend("/topic/tasks", "UPDATE");
    }
}