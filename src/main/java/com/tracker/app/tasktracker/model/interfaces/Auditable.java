package com.tracker.app.tasktracker.model.interfaces;
import java.time.LocalDateTime;

public interface Auditable {

    LocalDateTime getCreatedAt();
    void setCreatedAt(LocalDateTime createdAt);

}