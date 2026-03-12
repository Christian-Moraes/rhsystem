package com.rhsystem.domain.model;

public enum TimeEventStatus {
    CONFIRMED,  // Confiança alta - aprovado automaticamente
    REVIEW,     // Requer revisão manual
    UNCERTAIN   // Baixa confiança - precisa validação
}
