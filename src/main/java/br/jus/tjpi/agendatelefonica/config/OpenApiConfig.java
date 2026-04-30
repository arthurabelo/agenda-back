package br.jus.tjpi.agendatelefonica.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Agenda Telefonica API", version = "v1", description = "API da Agenda Telefonica do TJPI", contact = @Contact(name = "Equipe de Desenvolvimento", email = "giovanny.castro@tjpi.jus.br")))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    @Bean
    OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    GroupedOpenApi agendaPublicaApi() {
        return GroupedOpenApi.builder()
                .group("agenda-publica")
                .displayName("Agenda Publica")
                .pathsToMatch("/**")
                .pathsToExclude("/actuator")
                .build();
    }

    @Bean
    GroupedOpenApi monitoramentoApi() {
        return GroupedOpenApi.builder()
                .group("monitoramento")
                .displayName("Monitoramento")
                .pathsToMatch("/actuator")
                .build();
    }
}
