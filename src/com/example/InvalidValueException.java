package com.example;

public class InvalidValueException extends Exception {
    public InvalidValueException() {}

    public InvalidValueException(String message) {
        super(message);
    }
}

