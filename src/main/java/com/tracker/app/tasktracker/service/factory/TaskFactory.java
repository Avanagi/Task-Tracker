package com.tracker.app.tasktracker.service.factory;

import com.tracker.app.tasktracker.dto.TaskCreateDto;
import com.tracker.app.tasktracker.exception.IllegalTaskArgumentException;
import com.tracker.app.tasktracker.model.entity.tasks.AbstractTask;
import com.tracker.app.tasktracker.model.entity.users.User;
import com.tracker.app.tasktracker.repository.UserRepository;
import com.tracker.app.tasktracker.service.strategies.TaskCreationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskFactory {

    private final UserRepository userRepository;
    private final List<TaskCreationStrategy> strategies;

    public AbstractTask createTask(TaskCreateDto dto, User reporter) {
        TaskCreationStrategy strategy = strategies.stream()
                .filter(s -> s.getType().equals(dto.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalTaskArgumentException("Unknown task's type " + dto.getType()));

        AbstractTask task = strategy.create(dto);
        log.info("Created task using strategy: {}", strategy.getType());

        if (dto.getAssigneeIds() != null && !dto.getAssigneeIds().isEmpty()) {
            task.setAssignees(new HashSet<>(userRepository.findAllById(dto.getAssigneeIds())));
        }

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setDueDate(dto.getDueDate());
        task.setReporter(reporter);

        return task;
    }
}