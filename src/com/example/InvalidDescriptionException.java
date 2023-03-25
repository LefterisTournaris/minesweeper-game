package com.example;

public class InvalidDescriptionException extends Exception {
    public InvalidDescriptionException() {}

    public InvalidDescriptionException(String message) {
        super(message);
    }
}

