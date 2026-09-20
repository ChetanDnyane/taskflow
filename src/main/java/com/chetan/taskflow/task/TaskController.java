package com.chetan.taskflow.task;

// <editor-fold defaultstate="collapsed" desc="HTTP adapter for authenticated task operations">
/*
 * Maps POST/list GET to /api/tasks and item GET/PUT/DELETE to /api/tasks/{id}. JSON bodies are validated
 * before service calls; Spring converts the path ID to Long. The service owns business rules and
 * user scoping. Create returns 201; reads and updates return 200; successful delete returns 204
 * without a body. Missing/foreign tasks are mapped to 404 by GlobalExceptionHandler.
 */
// </editor-fold>

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request) {

        TaskResponse response = taskService.createTask(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getMyTasks() {

        List<TaskResponse> tasks = taskService.getMyTasks();

        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(
            @PathVariable Long id) {

        TaskResponse response = taskService.getTaskById(id);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {

        TaskResponse response = taskService.updateTask(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {

        taskService.deleteTask(id);

        return ResponseEntity.noContent().build();
    }
}