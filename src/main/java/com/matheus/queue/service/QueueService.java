package com.matheus.queue.service;

import com.matheus.queue.model.Status;
import com.matheus.queue.model.Task;
import com.matheus.queue.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import java.time.Clock;           
import java.time.Instant; 

@Service
public class QueueService {

    /** Higher priority first; same priority → FIFO (oldest createdAt first). */
    private static final Comparator<Task> NEXT_TO_SERVE =
            Comparator.comparing((Task t) -> t.getPriority().queueWeight()).reversed()
                    .thenComparing(Task::getCreatedAt);

    private final TaskRepository repository;
    private final Clock clock;

    public QueueService(TaskRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** PENDING cujo agendamento já venceu (ou sem agendamento). */
    private List<Task> eligiblePending(Instant now) {
        return repository.findByStatus(Status.PENDING).stream()
                .filter(t -> t.getScheduledFor() == null || !t.getScheduledFor().isAfter(now))
                .toList();
    }

    public List<Task> listPendingOrdered() {
        Instant now = Instant.now(clock);
        return eligiblePending(now).stream()
                .sorted(NEXT_TO_SERVE)
                .toList();
    }

    public Optional<Task> peekNext() {
        Instant now = Instant.now(clock);
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
