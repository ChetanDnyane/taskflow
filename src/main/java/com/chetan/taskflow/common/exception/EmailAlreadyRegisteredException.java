package com.chetan.taskflow.common.exception;

// <editor-fold defaultstate="collapsed" desc="Signal a duplicate registration">
/*
 * UserService throws this unchecked exception when the normalized email already exists.
 * GlobalExceptionHandler converts it to HTTP 409 and EMAIL_ALREADY_REGISTERED. Keeping this as a
 * domain exception lets the service report the problem without constructing an HTTP response.
 */
// </editor-fold>

public class EmailAlreadyRegisteredException
        extends RuntimeException {

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}
