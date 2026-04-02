package com.matheus.queue.controller;

import com.matheus.queue.dto.CreateTaskRequest;
import com.matheus.queue.model.Task;
import com.matheus.queue.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/tasks")
@Tag(name = "Solicitações", description = "Cadastrar pedidos e encerrar atendimentos")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service){
        this.service = service;
    }

    @Operation(summary = "Incluir na fila", description = "Registra um novo pedido (fica aguardando ou agendado).")
    @PostMapping
    public Task create(@Valid @RequestBody CreateTaskRequest request){
        return service.createTask(request.description(), request.priority(), request.scheduledFor());
    }

    @Operation(summary = "Ver tudo", description = "Lista todas as solicitações em qualquer situação (inclui agendadas para depois).")
    @GetMapping
    public List<Task> list(){
        return service.getAllTasks();
    }

    @Operation(summary = "Concluir atendimento", description = "Marca como finalizado um pedido que já está em atendimento.")
    @PostMapping("/{id}/complete")
    public Task complete(
            @Parameter(description = "Número do chamado", example = "1") @PathVariable Long id) {
        return service.completeTask(id);
    }
}
