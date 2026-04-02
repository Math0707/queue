package com.matheus.queue.model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Um pedido ou pessoa na fila")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Número do chamado", example = "1")
    private Long id;

    @Schema(description = "O que a pessoa precisa ou nome", example = "Maria — consulta")
    private String description;

    @Enumerated(EnumType.STRING)
    @Schema(description = "Urgência")
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Schema(description = "Situação atual")
    private Status status;

    @Schema(description = "Momento em que entrou no sistema")
    private LocalDateTime createdAt;

    @Schema(description = "Momento em que foi concluído", nullable = true)
    private LocalDateTime processedAt;

    @Schema(
            description = "Se preenchido, só entra na fila de chamada a partir deste horário",
            nullable = true)
    private LocalDateTime scheduledFor;

}
