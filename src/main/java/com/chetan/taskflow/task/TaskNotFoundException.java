package com.chetan.taskflow.task;

// <editor-fold defaultstate="collapsed" desc="Report an unavailable task without revealing ownership">
/*
 * The service throws this when findByIdAndUserId returns empty, whether the ID is missing or belongs
 * to somebody else. The message includes the requested ID only. GlobalExceptionHandler maps it to
 * HTTP 404 with TASK_NOT_FOUND, giving both situations the same external behavior.
 */
// </editor-fold>

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long taskId) {
        super("Task not found with id: " + taskId);
    }
}