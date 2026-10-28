package com.matheus.queue.controller;

import com.jayway.jsonpath.JsonPath;
import com.matheus.queue.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;


import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a aplicação inteira (perfil dev, H2 em memória) e testa a API via HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
class QueueApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TaskRepository repository;
    

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private ResultActions createTask(String json) throws Exception {
        return mvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions createTask(String description, String priority) throws Exception {
        return createTask("""
                {"description": "%s", "priority": "%s"}
                """.formatted(description, priority));
    }

    private static String iso(Instant instant) {
        return instant.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    @Test
    @DisplayName("Fluxo completo: criar → fila ordenada → chamar → concluir")
    void fullFlow() throws Exception {
        createTask("Ana - baixa", "LOW").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.id").value(notNullValue()));
        createTask("Bruno - alta", "HIGH").andExpect(status().isOk());

        mvc.perform(get("/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].description").value("Bruno - alta"))
                .andExpect(jsonPath("$[1].description").value("Ana - baixa"));

        // Só olhar não tira ninguém da fila
        mvc.perform(get("/queue/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Bruno - alta"));
        mvc.perform(get("/queue")).andExpect(jsonPath("$", hasSize(2)));

        String claimed = mvc.perform(post("/queue/claim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Bruno - alta"))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(claimed, "$.id");

        mvc.perform(get("/queue"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Ana - baixa"));

        mvc.perform(post("/tasks/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.processedAt").value(notNullValue()));

        // Concluir duas vezes é conflito
        mvc.perform(post("/tasks/{id}/complete", id))
                .andExpect(status().isConflict());

        mvc.perform(get("/tasks")).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Agendado para o futuro aparece em /tasks, mas não na fila nem no claim")
    void scheduledTaskStaysOutOfQueue() throws Exception {
        createTask("""
                {"description": "Carlos - retorno", "priority": "HIGH", "scheduledFor": "%s"}
                """.formatted(iso(Instant.now().plus(1, ChronoUnit.DAYS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduledFor").value(notNullValue()));

        mvc.perform(get("/tasks")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/queue")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/queue/next")).andExpect(status().isNotFound());
        mvc.perform(post("/queue/claim")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Fila vazia: next e claim devolvem 404")
    void emptyQueue() throws Exception {
        mvc.perform(get("/queue")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/queue/next")).andExpect(status().isNotFound());
        mvc.perform(post("/queue/claim")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Campos obrigatórios ausentes: 400 com erro por campo")
    void validation_missingFields() throws Exception {
        createTask("""
                {"description": "   "}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.priority").exists());

        mvc.perform(get("/tasks")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Agendamento no passado: 400")
    void validation_pastSchedule() throws Exception {
        createTask("""
                {"description": "Diego", "priority": "LOW", "scheduledFor": "%s"}
                """.formatted(iso(Instant.now().minus(1, ChronoUnit.DAYS))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.scheduledFor").exists());
    }

    @Test
    @DisplayName("Prioridade inexistente: 400")
    void validation_unknownPriority() throws Exception {
        createTask("Elisa", "URGENT").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Concluir quem ainda está aguardando: 409")
    void complete_pendingTask_conflict() throws Exception {
        String body = createTask("Fabio", "MEDIUM")
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.id");

        mvc.perform(post("/tasks/{id}/complete", id)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Concluir chamado inexistente: 404")
    void complete_unknownTask_notFound() throws Exception {
        mvc.perform(post("/tasks/{id}/complete", 999_999)).andExpect(status().isNotFound());
    }
}
