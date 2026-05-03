package com.tracker.app.tasktracker.model.interfaces;

import com.tracker.app.tasktracker.model.entity.users.User;
import java.util.Set;

public interface Assignable {

    Set<User> getAssignees();
    void setAssignees(Set<User> assignees);

}