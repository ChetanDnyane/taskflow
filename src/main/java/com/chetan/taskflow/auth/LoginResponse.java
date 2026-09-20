package com.chetan.taskflow.auth;

// <editor-fold defaultstate="collapsed" desc="Successful login JSON contract">
/*
 * Jackson serializes this record as an object containing token. Clients pass that value in the
 * Authorization header as Bearer followed by a space and the token for later protected requests.
 * There is no refresh token or server-side session identifier in this response.
 */
// </editor-fold>

public record LoginResponse(
        String token
) {
}