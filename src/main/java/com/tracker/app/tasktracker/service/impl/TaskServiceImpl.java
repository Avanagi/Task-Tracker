package com.tracker.app.tasktracker.service.impl;

import com.tracker.app.tasktracker.dto.TaskCreateDto;
import com.tracker.app.tasktracker.event.TaskAssigneesChangedEvent;
import com.tracker.app.tasktracker.event.TaskCreatedEvent;
import com.tracker.app.tasktracker.event.TaskDeletedEvent;
import com.tracker.app.tasktracker.event.TaskStatusChangedEvent;
import com.tracker.app.tasktracker.exception.InvalidTaskOperationException;
import com.tracker.app.tasktracker.exception.SubtaskNotFoundException;
import com.tracker.app.tasktracker.exception.TaskNotFoundException;
import com.tracker.app.tasktracker.exception.UserNotFoundException;
import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import com.tracker.app.tasktracker.model.entity.tasks.EpicTask;
import com.tracker.app.tasktracker.model.entity.tasks.Subtask;
import com.tracker.app.tasktracker.model.entity.users.User;
import com.tracker.app.tasktracker.model.enums.TaskStatus;
import com.tracker.app.tasktracker.repository.TaskRepository;
import com.tracker.app.tasktracker.repository.UserRepository;
import com.tracker.app.tasktracker.service.TaskService;
import com.tracker.app.tasktracker.service.factory.TaskFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskFactory taskFactory;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AbstractTask createTask(TaskCreateDto dto, String currentUsername) {
        User reporter = userRepository.findByUsername(currentUsername).orElseThrow(() -> new UserNotFoundException("User not found"));

        AbstractTask task = taskFactory.createTask(dto, reporter);
        AbstractTask savedTask = taskRepository.save(task);

        log.info("Task created ID: {}", savedTask.getId());
        eventPublisher.publishEvent(new TaskCreatedEvent(this, savedTask));
        return savedTask;
    }

    @Override
    @Transactional
    public AbstractTask changeStatus(Long taskId, TaskStatus newStatus, String currentUsername) {
        AbstractTask task = getTaskById(taskId);
        User currentUser = getCurrentUser(currentUsername);

        boolean isReporter = task.getReporter().getId().equals(currentUser.getId());
        boolean isAssignee = task.getAssignees().stream().anyMatch(a -> a.getId().equals(currentUser.getId()));

        if (!isReporter && !isAssignee) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only author or assignee can change status");
        }

        if (newStatus == TaskStatus.DONE && !isReporter) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only author can mark task as DONE");
        }

        if (task.getStatus() == TaskStatus.DONE && newStatus != TaskStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task is already DONE. Move to IN_PROGRESS to edit.");
        }

        if (task.getDueDate().isBefore(LocalDateTime.now()) && !isReporter) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task overdue! Only author can edit.");
        }

        task.setStatus(newStatus);
        AbstractTask updatedTask = taskRepository.save(task);
        eventPublisher.publishEvent(new TaskStatusChangedEvent(this, updatedTask));
        return updatedTask;
    }

    @Override
    @Transactional
    public AbstractTask assignUser(Long taskId, Long userId, String currentUsername) {
        AbstractTask task = getTaskById(taskId);
        checkTaskNotDone(task);

        if (!task.getReporter().getId().equals(getCurrentUser(currentUsername).getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only author can assign users");
        }

        User userToAssign = userRepository.findById(userId).orElseThrow();
        task.getAssignees().add(userToAssign);
        AbstractTask updatedTask = taskRepository.save(task);
        eventPublisher.publishEvent(new TaskAssigneesChangedEvent(this, updatedTask, userToAssign, "ASSIGNED"));
        return updatedTask;
    }

    @Override
    @Transactional
    public AbstractTask assignBulk(Long taskId, List<Long> userIds, String currentUsername) {
        AbstractTask task = getTaskById(taskId);

        User currentUser = getCurrentUser(currentUsername);
        if (!task.getReporter().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only author can assign users");
        }

        Set<User> oldAssignees = new HashSet<>(task.getAssignees());
        List<Long> idsToAssign = (userIds == null) ? List.of() : userIds;
        Set<User> newAssignees = new HashSet<>(userRepository.findAllById(idsToAssign));

        task.setAssignees(newAssignees);
        AbstractTask updatedTask = taskRepository.save(task);

        for (User user : newAssignees) {
            if (!oldAssignees.contains(user)) {
                eventPublisher.publishEvent(new TaskAssigneesChangedEvent(this, updatedTask, user, "ASSIGNED"));
            }
        }

        for (User user : oldAssignees) {
            if (!newAssignees.contains(user)) {
                eventPublisher.publishEvent(new TaskAssigneesChangedEvent(this, updatedTask, user, "REMOVED"));
            }
        }

        eventPublisher.publishEvent(new TaskStatusChangedEvent(this, updatedTask));

        return updatedTask;
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId, String currentUsername) {
        AbstractTask task = getTaskById(taskId);
        if (!task.getReporter().getId().equals(getCurrentUser(currentUsername).getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only author can delete task");
        }
        taskRepository.deleteById(taskId);
        eventPublisher.publishEvent(new TaskDeletedEvent(this, taskId));
    }

    @Override
    @Transactional
    public AbstractTask toggleSubtask(Long taskId, Long subtaskId, String currentUsername) {
        AbstractTask task = getTaskById(taskId);
        checkTaskNotDone(task);

        User currentUser = getCurrentUser(currentUsername);
        boolean isReporter = task.getReporter().getId().equals(currentUser.getId());
        boolean isAssignee = task.getAssignees().stream().anyMatch(a -> a.getId().equals(currentUser.getId()));

        if (!isReporter && !isAssignee) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }

        if (!(task instanceof EpicTask epic)) throw new InvalidTaskOperationException("Not an epic");

        Subtask subtask = epic.getSubtasks().stream()
                .filter(s -> s.getId().equals(subtaskId))
                .findFirst()
                .orElseThrow(() -> new SubtaskNotFoundException("Subtask with ID " + subtaskId + " not found"));
        subtask.setCompleted(!subtask.isCompleted());

        AbstractTask savedEpic = taskRepository.save(epic);
        eventPublisher.publishEvent(new TaskStatusChangedEvent(this, savedEpic));

        return savedEpic;
    }

    @Override
    @Transactional
    public AbstractTask updateDeadline(Long taskId, String dueDateStr, String currentUsername) {
        AbstractTask task = getTaskById(taskId);

        if (task.getStatus() == TaskStatus.DONE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to update deadline anymore");
        }

        if (!task.getReporter().getId().equals(getCurrentUser(currentUsername).getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only author can change deadline");
        }

        task.setDueDate(LocalDateTime.parse(dueDateStr));
        return taskRepository.save(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbstractTask> getAllTasks() {
        return taskRepository.findAll();
    }

    @Transactional
    private AbstractTask getTaskById(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> {
            log.error("Task not found with ID: {}", taskId);
            return new TaskNotFoundException("Task with ID " + taskId + " not found");
        });
    }

    @Transactional
    private User getCurrentUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException("User " + username + " not found"));
    }

    private void checkTaskNotDone(AbstractTask task) {
        if (task.getStatus() == TaskStatus.DONE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task is DONE");
    }
}