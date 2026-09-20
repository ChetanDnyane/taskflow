package com.chetan.taskflow.task;

// <editor-fold defaultstate="collapsed" desc="Database representation of a user-owned task">
/*
 * JPA maps this mutable entity to tasks; Lombok generates getters, setters and the no-argument
 * constructor required for persistence. The database generates the ID using an identity column.
 * Status/priority are stored as enum names rather than ordinals; renaming them affects stored data.
 * Every task has one required owner through user_id. LAZY delays loading that user until accessed.
 * Description and due date can be null. Hibernate fills createdAt on insertion and updatedAt on writes;
 * the due date is a LocalDate, while audit timestamps are Instants. APIs return TaskResponse instead
 * of serializing the entity and its lazy user relationship.
 */
// </editor-fold>

import com.chetan.taskflow.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}