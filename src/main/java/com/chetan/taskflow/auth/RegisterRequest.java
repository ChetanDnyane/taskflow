package com.chetan.taskflow.auth;

// <editor-fold defaultstate="collapsed" desc="Registration input and identity normalization">
/*
 * Jackson invokes the compact record constructor before controller Bean Validation. It strips outer
 * whitespace from name/email and lowercases email with Locale.ROOT for locale-independent identity.
 * Null guards preserve missing fields so NotBlank can report them. Passwords are never trimmed.
 * Name and email size limits match entity columns; password validation accepts 8 through 72 Java
 * characters. This is a character constraint, not an explicit UTF-8 byte-length check for BCrypt.
 */
// </editor-fold>

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name cannot exceed 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(
                min = 8,
                max = 72,
                message = "Password must contain between 8 and 72 characters"
        )
        String password
) {
        public RegisterRequest {
                if (name != null) {
                        name = name.strip();
                }

                if (email != null) {
                        email = email.strip().toLowerCase(Locale.ROOT);
                }
        }
}