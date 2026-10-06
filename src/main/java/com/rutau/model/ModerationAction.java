package com.rutau.model;

// US19 - Acciones que el administrador puede tomar sobre un reporte
public enum ModerationAction {
    WARNING,      // advertencia al usuario reportado
    SUSPENSION,   // se suspende la cuenta (ya no puede iniciar sesión)
    DISMISS       // el reporte no procede (se descarta)
}
