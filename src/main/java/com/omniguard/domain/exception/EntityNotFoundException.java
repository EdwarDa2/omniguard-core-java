package com.omniguard.domain.exception;

public class EntityNotFoundException extends RuntimeException {
    private final String entityType;
    private final String identifier;

    public EntityNotFoundException(String entityType, String identifier) {
        super(entityType + " no encontrado con identificador: " + identifier);
        this.entityType = entityType;
        this.identifier = identifier;
    }

    public EntityNotFoundException(String entityType, Object identifier) {
        this(entityType, identifier.toString());
    }

    public EntityNotFoundException(String message) {
        super(message);
        this.entityType = "Entity";
        this.identifier = message;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getIdentifier() {
        return identifier;
    }
}