package com.rutau.model;

public enum NotificationType {
    REQUEST_RECEIVED,
    REQUEST_ACCEPTED,
    REQUEST_REJECTED,
    REQUEST_CANCELLED,
    TRIP_CANCELLED,
    ACCOUNT_WARNING,     // US19 - el administrador envió una advertencia
    ACCOUNT_SUSPENDED    // US19 - el administrador suspendió la cuenta
}
