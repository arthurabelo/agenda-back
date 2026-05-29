package br.jus.tjpi.agendatelefonica.controller;

import br.jus.tjpi.agendatelefonica.dto.LoginRequest;
import br.jus.tjpi.agendatelefonica.dto.LoginResponse;
import br.jus.tjpi.agendatelefonica.model.Usuario;
import br.jus.tjpi.agendatelefonica.repository.UsuarioRepository;
import br.jus.tjpi.agendatelefonica.service.AuditLogService;
import br.jus.tjpi.agendatelefonica.service.LoginService;
import br.jus.tjpi.agendatelefonica.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioControllerTest {

    private LoginService loginService;
    private UsuarioRepository usuarioRepository;
    private TokenService tokenService;
    private AuditLogService auditLogService;

    private UsuarioController usuarioController;

    @BeforeEach
    void setUp() throws Exception {
        loginService = mock(LoginService.class);
        usuarioRepository = mock(UsuarioRepository.class);
        tokenService = mock(TokenService.class);
        auditLogService = mock(AuditLogService.class);
        usuarioController = new UsuarioController();

        inject("loginService", loginService);
        inject("repository", usuarioRepository);
        inject("tokenService", tokenService); // Injeta o gerador de token fake pro teste
        inject("auditLogService", auditLogService);
    }

    @Test
    void loginDeveRetornarDtoSemSenhaECriarCookie() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("usuario_teste");
        usuario.setPassword("segredo");
        usuario.setRole("USER");
        usuario.setActive(true);

        when(loginService.autenticarOuFalhar("usuario_teste", "segredo")).thenReturn(usuario);
        when(tokenService.gerarToken(usuario)).thenReturn("fake-jwt-token"); // Simula a geração do token

        // Cria a resposta simulada que será preenchida pelo Controller
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();

        // Passa a requisição e a resposta simulada
        ResponseEntity<LoginResponse> response = usuarioController.login(new LoginRequest("usuario_teste", "segredo"), mockResponse, mockRequest);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1L, response.getBody().id());
        assertEquals("usuario_teste", response.getBody().username());
        assertEquals("USER", response.getBody().role());
        assertEquals(true, response.getBody().active());

        // Valida extra para a segurança do TJPI: Garante que o cookie foi gerado corretamente!
        assertNotNull(mockResponse.getCookie("jwt_token"));
        assertEquals("fake-jwt-token", mockResponse.getCookie("jwt_token").getValue());
    }

    @Test
    void loginDevePropagarExcecaoDoServico() {
        RuntimeException expected = new RuntimeException("Falha de autenticação");
        when(loginService.autenticarOuFalhar("usuario_teste", "segredo")).thenThrow(expected);

        // Cria a resposta simulada para esse cenário também
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> usuarioController.login(new LoginRequest("usuario_teste", "segredo"), mockResponse, mockRequest));

        assertEquals(expected.getMessage(), thrown.getMessage());
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field field = UsuarioController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(usuarioController, value);
    }
}