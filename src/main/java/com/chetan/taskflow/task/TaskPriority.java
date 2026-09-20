package com.chetan.taskflow.task;

// <editor-fold defaultstate="collapsed" desc="Allowed task priorities">
/*
 * LOW, MEDIUM and HIGH are used as JSON enum strings and stored as text in the tasks table.
 * Creation defaults to MEDIUM; update requires an explicit priority. This enum defines labels only:
 * it does not automatically sort lists or affect task scheduling.
 */
// </editor-fold>

public enum TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}