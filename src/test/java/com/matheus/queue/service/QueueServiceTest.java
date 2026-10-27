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

import java.time.LocalDateTime;
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

    private QueueService service;

    private final LocalDateTime base = LocalDateTime.now().minusHours(1);

    @BeforeEach
    void setUp() {
        service = new QueueService(repository);
    }

    private static Task pending(long id, Priority priority, LocalDateTime createdAt, LocalDateTime scheduledFor) {
        return Task.builder()
                .id(id)
                .description("task " + id)
                .priority(priority)
                .status(Status.PENDING)
                .createdAt(createdAt)
                .scheduledFor(scheduledFor)
                .build();
    }

    private static Task pending(long id, Priority priority, LocalDateTime createdAt) {
        return pending(id, priority, createdAt, null);
    }

    @Test
    @DisplayName("Ordena por prioridade (HIGH > MEDIUM > LOW) e, no empate, por ordem de chegada")
    void listPendingOrdered_ordersByPriorityThenArrival() {
        when(repository.findByStatus(Status.PENDING)).thenReturn(List.of(
                pending(1, Priority.LOW, base),
                pending(2, Priority.HIGH, base.plusMinutes(2)),
                pending(3, Priority.MEDIUM, base.plusMinutes(1)),
                pending(4, Priority.HIGH, base.plusMinutes(1))
        ));

        List<Task> result = service.listPendingOrdered();

        assertThat(result).extracting(Task::getId).containsExactly(4L, 2L, 3L, 1L);
    }

    @Test
    @DisplayName("Agendamento no futuro fica fora da fila; agendamento vencido entra")
    void listPendingOrdered_respectsSchedule() {
        LocalDateTime now = LocalDateTime.now();
        when(repository.findByStatus(Status.PENDING)).thenReturn(List.of(
                pending(1, Priority.HIGH, base, now.plusHours(1)),
                pending(2, Priority.LOW, base, now.minusMinutes(1)),
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
                pending(2, Priority.HIGH, base.plusMinutes(5))
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
                pending(1, Priority.HIGH, base, LocalDateTime.now().plusDays(1))));

        assertThat(service.claimNext()).isEmpty();
        verify(repository, never()).save(any());
    }
}
