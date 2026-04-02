package com.matheus.queue.dto;

import com.matheus.queue.model.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(name = "NovaSolicitacao", description = "Dados para a pessoa ou o pedido entrar na fila")
public record CreateTaskRequest(
        @Schema(description = "Nome da pessoa ou resumo do que precisa", example = "João Silva — segunda via de cartão")
        @NotBlank
        String description,

        @Schema(description = "Urgência na fila", example = "MEDIUM", allowableValues = {"LOW", "MEDIUM", "HIGH"})
        @NotNull
        Priority priority,

        @Schema(
                description = "Opcional. Só aparece para atendimento a partir deste horário (data/hora local).",
                example = "2026-04-02T15:00:00",
                nullable = true)
        @Future
        LocalDateTime scheduledFor
) {}

