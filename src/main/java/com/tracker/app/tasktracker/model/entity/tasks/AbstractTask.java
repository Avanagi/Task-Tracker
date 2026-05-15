package com.tracker.app.tasktracker.model.entity.tasks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tracker.app.tasktracker.model.entity.users.User;
import com.tracker.app.tasktracker.model.enums.TaskStatus;
import com.tracker.app.tasktracker.model.interfaces.Assignable;
import com.tracker.app.tasktracker.model.interfaces.Auditable;
import com.tracker.app.tasktracker.model.interfaces.TaskTypeable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "tasks")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "task_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@ToString
@NoArgsConstructor
public abstract class AbstractTask implements Auditable, Assignable, TaskTypeable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    @JsonProperty("type")
    public abstract String getType();

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁ0-9\\s!.,_\\-]+$",
            message = "Title contains invalid characters")
    @Column(nullable = false)
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Column(length = 2000)
    private String description;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TODO;

    @PastOrPresent(message = "Created date cannot be in the future")
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    @Column(nullable = false)
    private LocalDateTime dueDate;

    @NotNull(message = "Reporter is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    @JsonIgnoreProperties({"password", "email", "hibernateLazyInitializer", "handler"})
    @ToString.Exclude
    private User reporter;

    @Size(max = 20, message = "Cannot assign more than 20 users to a task")
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_assignees",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "user_id"})
    )
    @JsonIgnoreProperties({"password", "email", "hibernateLazyInitializer", "handler"})
    @ToString.Exclude
    private Set<User> assignees = new HashSet<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AbstractTask that = (AbstractTask) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}