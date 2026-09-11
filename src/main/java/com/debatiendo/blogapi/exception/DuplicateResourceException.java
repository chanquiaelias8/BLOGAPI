package com.debatiendo.blogapi.exception;

/** El recurso viola una restriccion de unicidad de negocio (username/email ya tomados). */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
