package com.tracker.app.tasktracker.exception;

public class InvalidTaskOperationException extends RuntimeException {
    public InvalidTaskOperationException(String message) {
        super(message);
    }
}