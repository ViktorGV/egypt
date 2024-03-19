package com.uber.egypt.signature;

public class InvalidDocumentFormatException extends RuntimeException {

    public InvalidDocumentFormatException(Exception e) {
        super(e);
    }
}
