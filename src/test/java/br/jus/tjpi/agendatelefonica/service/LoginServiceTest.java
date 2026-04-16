package br.jus.tjpi.agendatelefonica.service;

import br.jus.tjpi.agendatelefonica.config.AdProperties;
import br.jus.tjpi.agendatelefonica.dto.AdUserInfo;
import br.jus.tjpi.agendatelefonica.exception.AuthConfigurationException;
import br.jus.tjpi.agendatelefonica.exception.InvalidCredentialsException;
import br.jus.tjpi.agendatelefonica.exception.InvalidPasswordException;
import br.jus.tjpi.agendatelefonica.exception.UserNotInGroupException;
import br.jus.tjpi.agendatelefonica.model.Usuario;
import br.jus.tjpi.agendatelefonica.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final String TEST_USERNAME = "usuario_teste";
    private static final String TEST_PASSWORD = "segredo";
    private static final String TEST_GROUP = "G.stic.agendatelefonica.usuarios";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ActiveDirectoryService activeDirectoryService;

    private AdProperties adProperties;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        adProperties = new AdProperties();
        adProperties.setLoginGroup(TEST_GROUP);
        adProperties.setServerUrl("ldap://ad.local:389");
        adProperties.setBaseDn("DC=ad,DC=local");
        loginService = new LoginService(usuarioRepository, activeDirectoryService, adProperties);
    }

    @Test
    void autenticarOuFalharDeveRetornarUsuarioLocalQuandoSenhaConfere() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setUsername(TEST_USERNAME);
        usuario.setPassword(TEST_PASSWORD);
        usuario.setRole("USER");
        usuario.setActive(true);

        when(usuarioRepository.findFirstByUsernameIgnoreCase(TEST_USERNAME)).thenReturn(Optional.of(usuario));

        Usuario result = loginService.autenticarOuFalhar(TEST_USERNAME, TEST_PASSWORD);

        assertEquals(usuario, result);
        verify(activeDirectoryService, never()).autenticarEObterUsuarioAd(TEST_USERNAME, TEST_PASSWORD);
    }

    @Test
    void autenticarOuFalharDeveRetornarUsuarioAdQuandoNaoExisteNoBancoEGrupoConfere() {
        AdUserInfo adUserInfo = new AdUserInfo("Usuário Teste", TEST_USERNAME, TEST_USERNAME + "@ad.local", List.of(TEST_GROUP));

        when(usuarioRepository.findFirstByUsernameIgnoreCase(TEST_USERNAME)).thenReturn(Optional.empty());
        when(activeDirectoryService.autenticarEObterUsuarioAd(TEST_USERNAME, TEST_PASSWORD)).thenReturn(Optional.of(adUserInfo));
        when(activeDirectoryService.usuarioPertenceAoGrupo(adUserInfo, TEST_GROUP))
                .thenReturn(true);

        Usuario result = loginService.autenticarOuFalhar(TEST_USERNAME, TEST_PASSWORD);

        assertEquals(TEST_USERNAME, result.getUsername());
        assertEquals("USER", result.getRole());
        assertTrue(result.isActive());
    }

    @Test
    void autenticarOuFalharDeveLancarErroQuandoNaoPassaNoBancoNemNoAd() {
        when(usuarioRepository.findFirstByUsernameIgnoreCase(TEST_USERNAME)).thenReturn(Optional.empty());
        when(activeDirectoryService.autenticarEObterUsuarioAd(TEST_USERNAME, TEST_PASSWORD)).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> loginService.autenticarOuFalhar(TEST_USERNAME, TEST_PASSWORD));
    }

    @Test
    void autenticarOuFalharDeveLancarErroQuandoNaoHouverGrupoConfigurado() {
        adProperties.setLoginGroup("   ");
        when(usuarioRepository.findFirstByUsernameIgnoreCase(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThrows(AuthConfigurationException.class,
                () -> loginService.autenticarOuFalhar(TEST_USERNAME, TEST_PASSWORD));
        verify(activeDirectoryService, never()).autenticarEObterUsuarioAd(TEST_USERNAME, TEST_PASSWORD);
    }

    @Test
    void autenticarOuFalharDeveLancarErroQuandoSenhaLocalIncorreta() {
        Usuario usuario = new Usuario();
        usuario.setUsername(TEST_USERNAME);
        usuario.setPassword("outra-senha");
        when(usuarioRepository.findFirstByUsernameIgnoreCase(TEST_USERNAME)).thenReturn(Optional.of(usuario));

        assertThrows(InvalidPasswordException.class,
                () -> loginService.autenticarOuFalhar(TEST_USERNAME, TEST_PASSWORD));
    }

    @Test
    void autenticarOuFalharDeveLancarErroQuandoUsuarioAdNaoPertenceAoGrupo() {
        AdUserInfo adUserInfo = new AdUserInfo("Usuário Teste", TEST_USERNAME, TEST_USERNAME + "@ad.local", List.of("OUTRO_GRUPO"));

        when(usuarioRepository.findFirstByUsernameIgnoreCase(TEST_USERNAME)).thenReturn(Optional.empty());
        when(activeDirectoryService.autenticarEObterUsuarioAd(TEST_USERNAME, TEST_PASSWORD)).thenReturn(Optional.of(adUserInfo));
        when(activeDirectoryService.usuarioPertenceAoGrupo(adUserInfo, TEST_GROUP)).thenReturn(false);

        assertThrows(UserNotInGroupException.class,
                () -> loginService.autenticarOuFalhar(TEST_USERNAME, TEST_PASSWORD));
    }
}