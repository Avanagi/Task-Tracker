package com.tracker.app.tasktracker.service.strategies;

import com.tracker.app.tasktracker.dto.TaskCreateDto;
import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import com.tracker.app.tasktracker.model.entity.tasks.EpicTask;
import com.tracker.app.tasktracker.model.entity.tasks.Subtask;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class EpicCreationStrategy implements TaskCreationStrategy {
    public AbstractTask create(TaskCreateDto dto) {
        EpicTask epic = new EpicTask();
        if (dto.getSubtaskTitles() != null) {
            epic.setSubtasks(dto.getSubtaskTitles().stream()
                    .map(title -> new Subtask(null, title, false))
                    .collect(Collectors.toList()));
        }
        return epic;
    }
    public String getType() { return "EPIC"; }
}