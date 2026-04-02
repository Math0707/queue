package com.matheus.queue.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Urgência na fila (Alta entra antes de Baixa)")
public enum Priority {
    @Schema(description = "Pode esperar mais")
    LOW,
    @Schema(description = "Ordinário")
    MEDIUM,
    @Schema(description = "Precisa passar antes")
    HIGH;

    public int queueWeight() {
        return switch (this) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }
}
