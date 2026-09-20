package com.chetan.taskflow.task;

// <editor-fold defaultstate="collapsed" desc="Allowed lifecycle labels">
/*
 * Tasks start at TODO and may be updated to IN_PROGRESS or COMPLETED. The enum is serialized and
 * persisted by name. No transition state machine is implemented: updates can choose any value,
 * including returning a completed task to TODO. Renaming values affects API clients and stored rows.
 */
// </editor-fold>

public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    COMPLETED
}