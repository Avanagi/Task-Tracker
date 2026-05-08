package com.tracker.app.tasktracker.event;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

@Slf4j
@Getter
public class TaskDeletedEvent extends ApplicationEvent {
    private final Long taskId;

    public TaskDeletedEvent(Object source, Long taskId) {
        super(source);
        this.taskId = taskId;
        log.info("Task with id {} has been removed", taskId);
    }

}