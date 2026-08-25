package com.hiresense.api.auth;

public class SlugAlreadyTakenException extends RuntimeException {

    public SlugAlreadyTakenException() {
        super("Organization slug already taken");
    }
}
