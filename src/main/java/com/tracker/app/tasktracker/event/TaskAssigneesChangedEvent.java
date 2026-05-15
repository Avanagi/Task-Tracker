package com.tracker.app.tasktracker.event;

import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import com.tracker.app.tasktracker.model.entity.users.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TaskAssigneesChangedEvent extends ApplicationEvent {
    private final AbstractTask task;
    private final User changedUser;
    private final String action;

    public TaskAssigneesChangedEvent(Object source, AbstractTask task, User changedUser, String action) {
        super(source);
        this.task = task;
        this.changedUser = changedUser;
        this.action = action;
    }
}