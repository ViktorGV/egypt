package com.uber.egypt.document;

public class InvalidDocumentFormatException extends RuntimeException {

    public InvalidDocumentFormatException(Exception e) {
        super(e);
    }
}
