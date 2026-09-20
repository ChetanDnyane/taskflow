package com.chetan.taskflow.task;

// <editor-fold defaultstate="collapsed" desc="Validated task creation input">
/*
 * A title is required and limited to 200 characters; description is optional up to 2000 characters.
 * Priority and dueDate may be absent. TaskService supplies MEDIUM and TODO defaults, trims the title,
 * and takes ownership from the authenticated user. Clients cannot supply an owner or initial status.
 * LocalDate represents a calendar date without a time zone; no future-date restriction is declared.
 */
// </editor-fold>

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTaskRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title cannot exceed 200 characters")
        String title,

        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,

        TaskPriority priority,

        LocalDate dueDate
) {
}