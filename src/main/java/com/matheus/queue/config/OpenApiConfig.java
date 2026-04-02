package com.matheus.queue.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fila Inteligente")
                        .description("""
                                API para filas com prioridade, agendamento e atendimento. \
                                Use o painel em / para o dia a dia; esta página é a documentação técnica.""")
                        .version("1.0.0")
                        .contact(new Contact().name("Queue Manager")))
                .tags(List.of(
                        new Tag()
                                .name("Fila de atendimento")
                                .description("Ordem do dia: quem pode ser chamado agora"),
                        new Tag()
                                .name("Solicitações")
                                .description("Cadastrar pedidos e encerrar atendimentos")));
    }
}
