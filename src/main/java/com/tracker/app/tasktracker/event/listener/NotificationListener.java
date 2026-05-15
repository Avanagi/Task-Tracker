package com.tracker.app.tasktracker.event.listener;

import com.tracker.app.tasktracker.event.TaskAssigneesChangedEvent;
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

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationListener {
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleTaskStatusChange(TaskStatusChangedEvent event) {
        AbstractTask task = event.getTask();
        String msg = "Task's status '" + task.getTitle() + "' set to " + task.getStatus();
        log.info(msg);
        if (task.getReporter() != null) sendToUser(task.getReporter(), msg);
        if (task.getAssignees() != null) {
            task.getAssignees().forEach(u -> sendToUser(u, msg));
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

    @EventListener
    public void handleAssigneesChanged(TaskAssigneesChangedEvent event) {
        User user = event.getChangedUser();
        AbstractTask task = event.getTask();

        String actionText = event.getAction().equals("ASSIGNED")
                ? "You were assigned to the task: "
                : "You were removed from the task: ";

        String msg = actionText + "'" + task.getTitle() + "'";

        messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/notifications",
                msg
        );

        messagingTemplate.convertAndSend("/topic/tasks", "UPDATE");

        log.info("Email sending to {}: {}", user.getUsername(), msg);
    }

    private void sendToUser(User user, String message) {
        messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/notifications", message);
    }
}