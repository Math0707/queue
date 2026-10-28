package com.matheus.queue.service;

import com.matheus.queue.model.Priority;
import com.matheus.queue.model.Status;
import com.matheus.queue.model.Task;
import com.matheus.queue.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository repository;

    private static final Instant NOW = Instant.parse("2026-11-03T18:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Instant base = NOW.minus(1, ChronoUnit.HOURS);

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(repository, clock);
    }

    private static Task withStatus(long id, Status status) {
        return Task.builder()
                .id(id)
                .description("task " + id)
                .priority(Priority.MEDIUM)
                .status(status)
                .createdAt(NOW.minus(10, ChronoUnit.MINUTES))
                .build();
    }

    @Test
    @DisplayName("Nova solicitação nasce PENDING, com data de criação e sem data de conclusão")
    void createTask_startsPending() {
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        Instant scheduled = NOW.plus(2, ChronoUnit.HOURS);
        Instant before = NOW;
        Task created = service.createTask("Maria — consulta", Priority.HIGH, scheduled);

        assertThat(created.getDescription()).isEqualTo("Maria — consulta");
        assertThat(created.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(created.getStatus()).isEqualTo(Status.PENDING);
        assertThat(created.getScheduledFor()).isEqualTo(scheduled);
        assertThat(created.getCreatedAt()).isAfterOrEqualTo(before);
        assertThat(created.getProcessedAt()).isNull();
    }

    @Test
    @DisplayName("Concluir quem está em atendimento: vira DONE e registra a data")
    void completeTask_fromProcessing() {
        Task task = withStatus(1, Status.PROCESSING);
        when(repository.findById(1L)).thenReturn(Optional.of(task));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task done = service.completeTask(1L);

        assertThat(done.getStatus()).isEqualTo(Status.DONE);
        assertThat(done.getProcessedAt()).isNotNull();
    }

    @ParameterizedTest(name = "status {0} não pode ser concluído")
    @EnumSource(value = Status.class, names = {"PENDING", "DONE", "ERROR"})
    @DisplayName("Concluir quem não está em atendimento devolve 409 e não salva")
    void completeTask_invalidStatus(Status status) {
        when(repository.findById(1L)).thenReturn(Optional.of(withStatus(1, status)));

        assertThatThrownBy(() -> service.completeTask(1L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Chamado inexistente devolve 404")
    void getById_notFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
