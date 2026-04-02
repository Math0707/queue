package com.matheus.queue.controller;

import com.matheus.queue.model.Task;
import com.matheus.queue.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/queue")
@Tag(name = "Fila de atendimento", description = "Ordem do dia: quem pode ser chamado agora")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @Operation(summary = "Quem está esperando agora", description = "Só quem já pode ser atendido, na ordem certa (prioridade e chegada).")
    @GetMapping
    public List<Task> listPending() {
        return queueService.listPendingOrdered();
    }

    @Operation(summary = "Próximo da fila (só olhar)", description = "Mostra o próximo sem chamar nem mudar nada.")
    @GetMapping("/next")
    public ResponseEntity<Task> peekNext() {
        return queueService.peekNext()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Chamar próximo", description = "Puxa o próximo da fila para atendimento (guichê / atendente).")
    @PostMapping("/claim")
    public ResponseEntity<Task> claimNext() {
        return queueService.claimNext()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
