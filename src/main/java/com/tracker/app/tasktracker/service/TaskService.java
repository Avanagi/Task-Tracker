package com.tracker.app.tasktracker.service;

import com.tracker.app.tasktracker.dto.TaskCreateDto;
import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import com.tracker.app.tasktracker.model.enums.TaskStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TaskService {

    @Transactional
    AbstractTask createTask(TaskCreateDto dto, String currentUsername);

    @Transactional
    AbstractTask changeStatus(Long taskId, TaskStatus newStatus, String currentUsername);

    @Transactional
    AbstractTask assignUser(Long taskId, Long userId, String currentUsername);

    @Transactional
    AbstractTask toggleSubtask(Long taskId, Long subtaskId);

    @Transactional
    AbstractTask assignBulk(Long id, List<Long> userIds, String name);

    @Transactional
    void deleteTask(Long id, String name);

    @Transactional
    List<AbstractTask> getAllTasks();
}