package com.chetan.taskflow.auth;

// <editor-fold defaultstate="collapsed" desc="Validated login JSON contract">
/*
 * The record provides immutable email/password values for AuthController. NotBlank rejects null,
 * empty and whitespace-only values; Email checks format. Validation runs because the controller uses
 * @Valid. Email normalization occurs in the controller after validation; the password is unchanged.
 */
// </editor-fold>

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,

        @NotBlank(message = "Password is required")
        String password

) {
}