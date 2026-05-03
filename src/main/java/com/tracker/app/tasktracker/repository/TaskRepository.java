package com.tracker.app.tasktracker.repository;

import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<AbstractTask, Long> {}
