package com.creditlens.service;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(Long id) {
        super("Credit application " + id + " was not found");
    }
}
