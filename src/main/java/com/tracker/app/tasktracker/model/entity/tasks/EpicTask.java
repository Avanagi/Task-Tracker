package com.tracker.app.tasktracker.model.entity.tasks;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@DiscriminatorValue("EPIC")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class EpicTask extends AbstractTask {

    @NotEmpty(message = "Epic must have at least one subtask")
    @Size(max = 50, message = "Epic cannot have more than 50 subtasks")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "epic_id")
    @ToString.Exclude
    private List<Subtask> subtasks = new ArrayList<>();

    @Override
    public String getType() {
        return "EPIC";
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        EpicTask epicTask = (EpicTask) o;
        return getId() != null && Objects.equals(getId(), epicTask.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}