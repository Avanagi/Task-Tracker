package com.tracker.app.tasktracker.service.strategies;

import com.tracker.app.tasktracker.dto.TaskCreateDto;
import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import com.tracker.app.tasktracker.model.entity.tasks.BugTask;
import org.springframework.stereotype.Component;

@Component
public class BugCreationStrategy implements TaskCreationStrategy {
    public AbstractTask create(TaskCreateDto dto) {
        BugTask bug = new BugTask();
        bug.setStepsToReproduce(dto.getStepsToReproduce());
        return bug;
    }
    public String getType() { return "BUG"; }
}