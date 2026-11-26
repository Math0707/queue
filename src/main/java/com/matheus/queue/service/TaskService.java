package com.matheus.queue.service;

import com.matheus.queue.model.Priority;
import com.matheus.queue.model.Status;
import com.matheus.queue.model.Task;
import com.matheus.queue.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository repository;
    private final Clock clock;

    public TaskService(TaskRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Task createTask(String description, Priority priority, Instant scheduledFor) {
        Instant now = Instant.now(clock);
        Task task = Task.builder()
                .description(description)
                .priority(priority)
                .status(Status.PENDING)
                .createdAt(now)
                .queuedAt(now)
                .scheduledFor(scheduledFor)
                .build();

        return repository.save(task);
    }

    public List<Task> getAllTasks() {
        return repository.findAll();
    }

    public Task getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    public Task completeTask(Long id) {
        Task task = getById(id);
        if (task.getStatus() != Status.PROCESSING) {
            String mensagem = switch (task.getStatus()) {
                case PENDING -> "Não é possível concluir: a pessoa ainda não foi chamada.";
                case DONE -> "Não é possível concluir: o atendimento já foi concluído.";
                case CANCELLED -> "Não é possível concluir: o atendimento foi cancelado.";
                case PROCESSING -> throw new IllegalStateException("PROCESSING deveria ter sido tratado antes do switch");
            };
            throw new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
        }
        task.setStatus(Status.DONE);
        task.setProcessedAt(Instant.now(clock));
        return repository.save(task);
    }

    public Task cancelTask(Long id) {
        Task task = getById(id);
        if (task.getStatus() != Status.PENDING) {
            String mensagem = switch (task.getStatus()) {
                case PROCESSING -> "Não é possível cancelar: a pessoa já está em atendimento.";
                case DONE -> "Não é possível cancelar: o atendimento já foi concluído.";
                case CANCELLED -> "Não é possível cancelar: o atendimento já foi cancelado.";
                case PENDING -> throw new IllegalStateException("PENDING deveria ter sido tratado antes do switch");
            };
            throw new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
        }
        task.setStatus(Status.CANCELLED);
        return repository.save(task);
    }
}
