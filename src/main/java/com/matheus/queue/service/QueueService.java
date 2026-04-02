package com.matheus.queue.service;

import com.matheus.queue.model.Status;
import com.matheus.queue.model.Task;
import com.matheus.queue.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class QueueService {

    /** Higher priority first; same priority → FIFO (oldest createdAt first). */
    private static final Comparator<Task> NEXT_TO_SERVE =
            Comparator.comparing((Task t) -> t.getPriority().queueWeight()).reversed()
                    .thenComparing(Task::getCreatedAt);

    private final TaskRepository repository;

    public QueueService(TaskRepository repository) {
        this.repository = repository;
    }

    /** PENDING cujo agendamento já venceu (ou sem agendamento). */
    private List<Task> eligiblePending(LocalDateTime now) {
        return repository.findByStatus(Status.PENDING).stream()
                .filter(t -> t.getScheduledFor() == null || !t.getScheduledFor().isAfter(now))
                .toList();
    }

    public List<Task> listPendingOrdered() {
        LocalDateTime now = LocalDateTime.now();
        return eligiblePending(now).stream()
                .sorted(NEXT_TO_SERVE)
                .toList();
    }

    public Optional<Task> peekNext() {
        LocalDateTime now = LocalDateTime.now();
        return eligiblePending(now).stream()
                .min(NEXT_TO_SERVE);
    }

    @Transactional
    public Optional<Task> claimNext() {
        return peekNext().map(task -> {
            task.setStatus(Status.PROCESSING);
            return repository.save(task);
        });
    }
}
