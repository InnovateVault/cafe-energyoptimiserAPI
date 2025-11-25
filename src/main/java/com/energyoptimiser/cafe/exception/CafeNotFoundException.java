package com.energyoptimiser.cafe.exception;

public class CafeNotFoundException extends RuntimeException {
    public CafeNotFoundException(Long id) {
        super("Cafe with ID " + id + " does not exist");
    }
}