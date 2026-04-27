package br.jus.tjpi.agendatelefonica.controller;

import br.jus.tjpi.agendatelefonica.dto.LoginRequest;
import br.jus.tjpi.agendatelefonica.dto.LoginResponse;
import br.jus.tjpi.agendatelefonica.model.Usuario;
import br.jus.tjpi.agendatelefonica.repository.UsuarioRepository;
import br.jus.tjpi.agendatelefonica.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioControllerTest {

    private LoginService loginService;
    private UsuarioRepository usuarioRepository;

        private UsuarioController usuarioController;

        @BeforeEach
        void setUp() throws Exception {
                loginService = mock(LoginService.class);
                usuarioRepository = mock(UsuarioRepository.class);
                usuarioController = new UsuarioController();

                inject("loginService", loginService);
                inject("repository", usuarioRepository);
        }

    @Test
        void loginDeveRetornarDtoSemSenha() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("usuario_teste");
        usuario.setPassword("segredo");
        usuario.setRole("USER");
        usuario.setActive(true);

        when(loginService.autenticarOuFalhar("usuario_teste", "segredo")).thenReturn(usuario);

                ResponseEntity<LoginResponse> response = usuarioController.login(new LoginRequest("usuario_teste", "segredo"));

                assertEquals(200, response.getStatusCode().value());
                assertEquals(1L, response.getBody().id());
                assertEquals("usuario_teste", response.getBody().username());
                assertEquals("USER", response.getBody().role());
                assertEquals(true, response.getBody().active());
    }

    @Test
        void loginDevePropagarExcecaoDoServico() {
                RuntimeException expected = new RuntimeException("Falha de autenticação");
                when(loginService.autenticarOuFalhar("usuario_teste", "segredo")).thenThrow(expected);

                RuntimeException thrown = assertThrows(RuntimeException.class,
                                () -> usuarioController.login(new LoginRequest("usuario_teste", "segredo")));

                assertEquals(expected.getMessage(), thrown.getMessage());
    }

        private void inject(String fieldName, Object value) throws Exception {
                Field field = UsuarioController.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(usuarioController, value);
    }
}
