package com.matheus.queue.service;

import com.matheus.queue.model.Priority;
import com.matheus.queue.model.Status;
import com.matheus.queue.model.Task;
import com.matheus.queue.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.Clock;  
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
        Task task = Task.builder()
                .description(description)
                .priority(priority)
                .status(Status.PENDING)
                .createdAt(Instant.now(clock))
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
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only tasks in PROCESSING can be completed; current status: " + task.getStatus());
        }
        task.setStatus(Status.DONE);
        task.setProcessedAt(Instant.now(clock));
        return repository.save(task);
    }
}