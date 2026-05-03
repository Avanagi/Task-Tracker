package com.tracker.app.tasktracker.service.impl;

import com.tracker.app.tasktracker.dto.TaskCreateDto;
import com.tracker.app.tasktracker.event.TaskCreatedEvent;
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
        log.debug("Creating task with DTO: {}", dto);

        User reporter = userRepository.findByUsername(currentUsername).orElseThrow(() -> {
            log.error("Current user not found: {}", currentUsername);
            return new UserNotFoundException("User " + currentUsername + " not found");
        });

        AbstractTask task = taskFactory.createTask(dto, reporter);
        log.debug("Task created by factory: {}", task);

        AbstractTask savedTask = taskRepository.save(task);
        log.info("Task created successfully - ID: {}, Type: {}, Time: {}", savedTask.getId(), dto.getType(), task.getCreatedAt());

        eventPublisher.publishEvent(new TaskCreatedEvent(this, savedTask));

        return savedTask;
    }

    @Override
    @Transactional
    public AbstractTask changeStatus(Long taskId, TaskStatus newStatus, String currentUsername) {
        log.debug("Changing status for task ID: {} to status: {} by user: {}", taskId, newStatus, currentUsername);

        AbstractTask task = getTaskById(taskId);
        User currentUser = getCurrentUser(currentUsername);

        boolean isReporter = task.getReporter().getId().equals(currentUser.getId());
        boolean isAssignee = task.getAssignees().stream().anyMatch(a -> a.getId().equals(currentUser.getId()));

        if (!isReporter && !isAssignee) {
            log.warn("Access denied. User {} tried to change status of task {}", currentUsername, taskId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only author or assignee can change status of task");
        }

        TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);
        AbstractTask updatedTask = taskRepository.save(task);

        log.info("Task status updated - Task ID: {}, Old status: {}, New status: {}", taskId, oldStatus, newStatus);
        eventPublisher.publishEvent(new TaskStatusChangedEvent(this, updatedTask));

        return updatedTask;
    }

    @Override
    @Transactional
    public AbstractTask assignUser(Long taskId, Long userId, String currentUsername) {
        log.debug("Assigning user ID: {} to task ID: {} by user: {}", userId, taskId, currentUsername);

        AbstractTask task = getTaskById(taskId);
        User currentUser = getCurrentUser(currentUsername);

        if (!task.getReporter().getId().equals(currentUser.getId())) {
            log.warn("Access denied. User {} tried to assign users to task {}", currentUsername, taskId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only author can assign users to task");
        }

        User userToAssign = userRepository.findById(userId).orElseThrow(() -> {
            userNotFoundId(userId);
            return new UserNotFoundException("User with ID " + userId + " not found");
        });

        Set<User> assignees = task.getAssignees();
        boolean isNewUser = assignees.add(userToAssign);

        if (isNewUser) {
            log.info("User ID: {} assigned to task ID: {}", userId, taskId);
        }

        AbstractTask updatedTask = taskRepository.save(task);
        eventPublisher.publishEvent(new TaskStatusChangedEvent(this, updatedTask));
        return updatedTask;
    }

    @Override
    @Transactional
    public AbstractTask assignBulk(Long taskId, List<Long> userIds, String currentUsername) {
        log.debug("Bulk assigning users to task ID: {} by user: {}", taskId, currentUsername);

        AbstractTask task = getTaskById(taskId);
        User currentUser = getCurrentUser(currentUsername);

        if (!task.getReporter().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only author can assign users to task");
        }

        List<Long> idsToAssign = (userIds == null) ? List.of() : userIds;
        task.setAssignees(new HashSet<>(userRepository.findAllById(idsToAssign)));

        AbstractTask updatedTask = taskRepository.save(task);
        log.info("Bulk assignment completed for task ID: {}", taskId);

        eventPublisher.publishEvent(new TaskStatusChangedEvent(this, updatedTask));
        return updatedTask;
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId, String currentUsername) {
        log.debug("Deleting task ID: {} by user: {}", taskId, currentUsername);

        AbstractTask task = getTaskById(taskId);
        User currentUser = getCurrentUser(currentUsername);

        if (!task.getReporter().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only author can remove users from task");
        }

        taskRepository.deleteById(taskId);
        log.info("Task ID: {} deleted successfully", taskId);
    }

    @Override
    @Transactional
    public AbstractTask toggleSubtask(Long taskId, Long subtaskId) {
        log.debug("Toggling subtask ID: {} for task ID: {}", subtaskId, taskId);

        AbstractTask task = getTaskById(taskId);

        if (!(task instanceof EpicTask epic)) {
            log.error("Invalid operation - Task ID: {} is not an EpicTask", taskId);
            throw new InvalidTaskOperationException("Subtasks are only available for epic tasks.");
        }

        Subtask subtask = epic.getSubtasks().stream().filter(s -> s.getId().equals(subtaskId)).findFirst().orElseThrow(() -> {
            log.error("Subtask not found - Subtask ID: {} in Epic ID: {}", subtaskId, taskId);
            return new SubtaskNotFoundException("Subtask with ID " + subtaskId + " not found");
        });

        subtask.setCompleted(!subtask.isCompleted());
        log.info("Subtask toggled - Epic ID: {}, Subtask ID: {}, New status: {}", taskId, subtaskId, subtask.isCompleted());

        return taskRepository.save(epic);
    }

    @Transactional
    private AbstractTask getTaskById(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> {
            taskNotFoundId(taskId);
            return new TaskNotFoundException("Task with ID " + taskId + " not found");
        });
    }

    @Transactional
    private User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User " + username + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbstractTask> getAllTasks() {
        log.debug("Fetching all tasks from database");
        return taskRepository.findAll();
    }

    private static void userNotFoundId(Long userId) { log.error("User not found with ID: {}", userId); }
    private static void taskNotFoundId(Long taskId) { log.error("Task not found with ID: {}", taskId); }
}