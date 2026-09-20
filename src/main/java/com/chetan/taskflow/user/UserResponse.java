package com.chetan.taskflow.user;

// <editor-fold defaultstate="collapsed" desc="Safe registration response">
/*
 * An immutable snapshot of the saved account with database ID, normalized name/email, role and
 * creation time. Password and passwordHash are deliberately absent so returning this record cannot
 * accidentally expose stored credentials. UserService performs the entity-to-record mapping.
 */
// </editor-fold>

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        Instant createdAt
) {
}