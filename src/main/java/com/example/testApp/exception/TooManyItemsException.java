package com.example.testApp.exception;

public class TooManyItemsException extends RuntimeException {

    public TooManyItemsException(String message) {
        super(message);
    }
}
