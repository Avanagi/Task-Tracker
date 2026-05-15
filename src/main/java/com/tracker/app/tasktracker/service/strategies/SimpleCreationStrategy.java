package com.tracker.app.tasktracker.service.strategies;

import com.tracker.app.tasktracker.dto.TaskCreateDto;
import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import com.tracker.app.tasktracker.model.entity.tasks.SimpleTask;
import org.springframework.stereotype.Component;

@Component
public class SimpleCreationStrategy implements TaskCreationStrategy {
    public AbstractTask create(TaskCreateDto dto) { return new SimpleTask(); }

    public String getType() { return "TASK"; }
}