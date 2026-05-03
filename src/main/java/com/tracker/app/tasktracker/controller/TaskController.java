package com.tracker.app.tasktracker.controller;

import com.tracker.app.tasktracker.dto.TaskCreateDto;
import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import com.tracker.app.tasktracker.model.enums.TaskStatus;
import com.tracker.app.tasktracker.service.TaskService;
import com.tracker.app.tasktracker.repository.TaskRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<AbstractTask>> getAllTasks() {
        log.info("Received request to get all tasks");
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @PostMapping
    public ResponseEntity<AbstractTask> createTask(@Valid @RequestBody TaskCreateDto dto, Principal principal) {
        log.info("Received request to create task by user: {}", principal.getName());
        return ResponseEntity.ok(taskService.createTask(dto, principal.getName()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AbstractTask> changeStatus(@PathVariable Long id, @RequestParam TaskStatus status, Principal principal) {
        log.info("Received request to change status of task {} by user: {}", id, principal.getName());
        return ResponseEntity.ok(taskService.changeStatus(id, status, principal.getName()));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<AbstractTask> assignUser(@PathVariable Long id, @RequestParam Long userId, Principal principal) {
        log.info("Received request to assign user {} to task {} by: {}", userId, id, principal.getName());
        return ResponseEntity.ok(taskService.assignUser(id, userId, principal.getName()));
    }

    @PatchMapping("/{id}/assign-bulk")
    public ResponseEntity<AbstractTask> assignBulk(@PathVariable Long id, @RequestParam(required = false) List<Long> userIds, Principal principal) {
        log.info("Received request to assign bulk for task {} by: {}", id, principal.getName());
        return ResponseEntity.ok(taskService.assignBulk(id, userIds, principal.getName()));
    }

    @PatchMapping("/{taskId}/subtasks/{subtaskId}/toggle")
    public ResponseEntity<AbstractTask> toggleSubtask(@PathVariable Long taskId, @PathVariable Long subtaskId) {
        log.info("Received request to toggle subtask {} for task {}", subtaskId, taskId);
        return ResponseEntity.ok(taskService.toggleSubtask(taskId, subtaskId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, Principal principal) {
        log.info("Received request to delete task {} by user: {}", id, principal.getName());
        taskService.deleteTask(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}