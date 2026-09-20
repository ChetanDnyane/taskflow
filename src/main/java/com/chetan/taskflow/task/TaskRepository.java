package com.chetan.taskflow.task;

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