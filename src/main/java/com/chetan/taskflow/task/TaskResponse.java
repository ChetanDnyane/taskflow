package com.chetan.taskflow.task;

// <editor-fold defaultstate="collapsed" desc="Public task representation">
/*
 * The immutable record is serialized as JSON after TaskService copies fields from a saved entity.
 * It includes business fields and audit timestamps, but omits the owner entity and its credentials.
 * Using a DTO prevents lazy relationship traversal and keeps the API separate from persistence.
 */
// </editor-fold>

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt
) {
}