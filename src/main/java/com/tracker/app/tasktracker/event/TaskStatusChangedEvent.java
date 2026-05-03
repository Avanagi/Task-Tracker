package com.tracker.app.tasktracker.event;

import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Getter
@Slf4j
public class TaskStatusChangedEvent extends ApplicationEvent {
    private final AbstractTask task;

    public TaskStatusChangedEvent(Object source, AbstractTask task) {
        super(source);
        this.task = task;
        log.info("Task {} has been changed", task.getTitle());
    }
}