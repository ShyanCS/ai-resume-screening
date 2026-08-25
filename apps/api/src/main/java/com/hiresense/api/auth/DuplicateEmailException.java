package com.hiresense.api.auth;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("Email already registered");
    }
}
