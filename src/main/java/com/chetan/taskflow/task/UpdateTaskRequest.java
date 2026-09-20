package com.chetan.taskflow.task;

// <editor-fold defaultstate="collapsed" desc="Full replacement of editable task fields">
/*
 * PUT requires title, status and priority; description and dueDate are optional and become null
 * when omitted. This is not a partial patch: TaskService assigns every editable field from this
 * record. Validation enforces title/description sizes before mutation. Ownership and audit fields
 * are excluded so callers cannot reassign a task or supply persistence timestamps.
 */
// </editor-fold>

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTaskRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title cannot exceed 200 characters")
        String title,

        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,

        @NotNull(message = "Status is required")
        TaskStatus status,

        @NotNull(message = "Priority is required")
        TaskPriority priority,

        LocalDate dueDate
) {
}