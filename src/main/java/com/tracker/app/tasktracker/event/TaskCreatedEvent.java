package com.tracker.app.tasktracker.event;

import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TaskCreatedEvent extends ApplicationEvent {
    private final AbstractTask task;

    public TaskCreatedEvent(Object source, AbstractTask task) {
        super(source);
        this.task = task;
    }
}