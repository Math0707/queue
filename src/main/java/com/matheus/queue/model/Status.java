package com.matheus.queue.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Situação do chamado")
public enum Status {
    @Schema(description = "Aguardando ou agendado")
    PENDING,
    @Schema(description = "Sendo atendido agora")
    PROCESSING,
    @Schema(description = "Finalizado com sucesso")
    DONE,
    @Schema(description = "Algo deu errado")
    ERROR
}
