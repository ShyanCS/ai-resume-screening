package com.hiresense.api.auth.email;

public class InvalidEmailTokenException extends RuntimeException {

    public InvalidEmailTokenException() {
        super("Invalid or expired token");
    }
}
