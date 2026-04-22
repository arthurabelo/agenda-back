package br.jus.tjpi.agendatelefonica.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/login");
    }

    @Test
    void handleInvalidCredentialsDeveRetornar401ComJson() {
        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleInvalidCredentials(
                new InvalidCredentialsException("Credenciais inválidas."),
                request
        );

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Unauthorized", response.getBody().error());
        assertEquals("Credenciais inválidas.", response.getBody().message());
        assertEquals("/login", response.getBody().path());
    }

    @Test
    void handleUserNotInGroupDeveRetornar403ComJson() {
        ResponseEntity<ApiErrorResponse> response = globalExceptionHandler.handleUserNotInGroup(
                new UserNotInGroupException("Usuário sem permissão."),
                request
        );

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Forbidden", response.getBody().error());
        assertEquals("Usuário sem permissão.", response.getBody().message());
        assertEquals("/login", response.getBody().path());
    }
}
