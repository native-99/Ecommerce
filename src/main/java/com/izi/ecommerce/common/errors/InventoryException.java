package com.izi.ecommerce.common.errors;

public class InventoryException extends RuntimeException {
    public InventoryException(String message) {
        super(message);
    }
}
