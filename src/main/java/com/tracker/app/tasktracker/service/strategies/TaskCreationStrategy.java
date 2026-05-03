package com.tracker.app.tasktracker.service.strategies;

import com.tracker.app.tasktracker.dto.TaskCreateDto;
import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;

public interface TaskCreationStrategy {
    AbstractTask create(TaskCreateDto dto);
    String getType();
}