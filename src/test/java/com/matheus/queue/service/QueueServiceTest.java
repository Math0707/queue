package com.matheus.queue.service;

import com.matheus.queue.model.Priority;
import com.matheus.queue.model.Status;
import com.matheus.queue.model.Task;
import com.matheus.queue.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock
    private TaskRepository repository;

    private static final Instant NOW = Instant.parse("2026-11-03T18:00:00Z");
    
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Instant base = NOW.minus(1, ChronoUnit.HOURS);

    private QueueService service;


    @BeforeEach
    void setUp() {
        service = new QueueService(repository, clock);
    }

    private static Task pending(long id, Priority priority, Instant createdAt, Instant scheduledFor) {
        return Task.builder()
                .id(id)
                .description("task " + id)
                .priority(priority)
                .status(Status.PENDING)
                .createdAt(createdAt)
                .scheduledFor(scheduledFor)
                .build();
    }

    private static Task pending(long id, Priority priority, Instant createdAt) {
        return pending(id, priority, createdAt, null);
    }

    @Test
    @DisplayName("Ordena por prioridade (HIGH > MEDIUM > LOW) e, no empate, por ordem de chegada")
    void listPendingOrdered_ordersByPriorityThenArrival() {
        when(repository.findByStatus(Status.PENDING)).thenReturn(List.of(
                pending(1, Priority.LOW, base),
                pending(2, Priority.HIGH, base.plus(2, ChronoUnit.HOURS)),
                pending(3, Priority.MEDIUM, base.plus(1, ChronoUnit.MINUTES)),
                pending(4, Priority.HIGH, base.plus(1, ChronoUnit.MINUTES))
        ));

        List<Task> result = service.listPendingOrdered();

        assertThat(result).extracting(Task::getId).containsExactly(4L, 2L, 3L, 1L);
    }

    @Test
    @DisplayName("Agendamento no futuro fica fora da fila; agendamento vencido entra")
    void listPendingOrdered_respectsSchedule() {
        when(repository.findByStatus(Status.PENDING)).thenReturn(List.of(
                pending(1, Priority.HIGH, base, NOW.plus(1, ChronoUnit.HOURS)),
                pending(2, Priority.LOW, base, NOW.minus(1, ChronoUnit.MINUTES)),
                pending(3, Priority.MEDIUM, base)
        ));

        List<Task> result = service.listPendingOrdered();

        assertThat(result).extracting(Task::getId).containsExactly(3L, 2L);
    }

    @Test
    @DisplayName("Próximo da fila é o de maior prioridade e só olhar não altera nada")
    void peekNext_returnsHighestWithoutSaving() {
        when(repository.findByStatus(Status.PENDING)).thenReturn(List.of(
                pending(1, Priority.MEDIUM, base),
                pending(2, Priority.HIGH, base.plus(5, ChronoUnit.MINUTES))
        ));

        Optional<Task> next = service.peekNext();

        assertThat(next).get().extracting(Task::getId).isEqualTo(2L);
        assertThat(next.get().getStatus()).isEqualTo(Status.PENDING);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Fila vazia: não há próximo")
    void peekNext_emptyQueue() {
        when(repository.findByStatus(Status.PENDING)).thenReturn(List.of());

        assertThat(service.peekNext()).isEmpty();
    }

    @Test
    @DisplayName("Chamar o próximo muda o status para PROCESSING e salva")
    void claimNext_marksAsProcessing() {
        Task high = pending(2, Priority.HIGH, base);
        when(repository.findByStatus(Status.PENDING)).thenReturn(List.of(
                pending(1, Priority.LOW, base), high));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Task> claimed = service.claimNext();

        assertThat(claimed).get().extracting(Task::getId).isEqualTo(2L);
        assertThat(claimed.get().getStatus()).isEqualTo(Status.PROCESSING);
        verify(repository).save(high);
    }

    @Test
    @DisplayName("Chamar o próximo com fila vazia não salva nada")
    void claimNext_emptyQueue() {
        when(repository.findByStatus(Status.PENDING)).thenReturn(List.of());

        assertThat(service.claimNext()).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Só agendados para o futuro: ninguém pode ser chamado")
    void claimNext_onlyFutureScheduled() {
        when(repository.findByStatus(Status.PENDING)).thenReturn(List.of(
                pending(1, Priority.HIGH, base, NOW.plus(1, ChronoUnit.DAYS))));

        assertThat(service.claimNext()).isEmpty();
        verify(repository, never()).save(any());
    }
}
