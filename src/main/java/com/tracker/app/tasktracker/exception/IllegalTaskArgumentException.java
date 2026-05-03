package com.tracker.app.tasktracker.exception;

public class IllegalTaskArgumentException extends RuntimeException {
    public IllegalTaskArgumentException(String message) {
        super(message);
    }
}