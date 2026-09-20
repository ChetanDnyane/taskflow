package com.chetan.taskflow.task;

// <editor-fold defaultstate="collapsed" desc="Persistence queries that enforce task ownership">
/*
 * Spring Data supplies the implementation of JpaRepository and inherited CRUD methods at runtime.
 * findByIdAndUserId traverses task.user.id and requires both the task ID and owner ID to match.
 * findByUserId returns only that owner's tasks; no ordering or pagination is requested.
 * Services use these scoped methods for user-facing reads and mutations. Inherited findById/findAll
 * are not ownership-aware and must not replace these scoped queries in protected workflows.
 */
// </editor-fold>

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    List<Task> findByUserId(Long userId);

//    Auto-created methods
//    save(task)
//    findById(id)
//    findAll()
//    delete(task)
//    count()
}