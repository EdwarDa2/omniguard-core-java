package com.omniguard.domain.exception;

public class AuthorizationDeniedException extends RuntimeException {
    private final String requiredRole;
    private final String currentRole;

    public AuthorizationDeniedException(String requiredRole, String currentRole) {
        super("Acceso denegado. Se requiere rol: " + requiredRole + ", actual: " + currentRole);
        this.requiredRole = requiredRole;
        this.currentRole = currentRole;
    }

    public String getRequiredRole() {
        return requiredRole;
    }

    public String getCurrentRole() {
        return currentRole;
    }
}