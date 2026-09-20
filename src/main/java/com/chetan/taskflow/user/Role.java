package com.chetan.taskflow.user;

// <editor-fold defaultstate="collapsed" desc="Application authorization roles">
/*
 * New registrations always receive USER. ADMIN is available for persisted accounts but is not
 * assignable through the registration request. CustomUserDetailsService converts these names into
 * ROLE_USER or ROLE_ADMIN authorities; SecurityConfig checks ADMIN on its configured admin path.
 */
// </editor-fold>

public enum Role {
    USER,
    ADMIN
}