package com.tracker.app.tasktracker.model.entity.tasks;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@DiscriminatorValue("BUG")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class BugTask extends AbstractTask {

    @NotBlank(message = "Steps to reproduce are required for bug ticket")
    @Size(min = 10, max = 5000, message = "Steps to reproduce must be between 10 and 5000 characters")
    @Column(length = 5000)
    private String stepsToReproduce;

    @Override
    public String getType() {
        return "BUG";
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        BugTask bugTask = (BugTask) o;
        return getId() != null && Objects.equals(getId(), bugTask.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}