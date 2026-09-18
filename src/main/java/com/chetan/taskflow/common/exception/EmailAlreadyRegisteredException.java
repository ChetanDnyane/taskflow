package com.chetan.taskflow.common.exception;

public class EmailAlreadyRegisteredException
        extends RuntimeException {

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}
