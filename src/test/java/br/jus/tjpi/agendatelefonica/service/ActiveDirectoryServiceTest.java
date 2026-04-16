package br.jus.tjpi.agendatelefonica.service;

import br.jus.tjpi.agendatelefonica.config.AdProperties;
import br.jus.tjpi.agendatelefonica.dto.AdUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveDirectoryServiceTest {

    private ActiveDirectoryService activeDirectoryService;

    @BeforeEach
    void setUp() {
        AdProperties adProperties = new AdProperties();
        activeDirectoryService = new ActiveDirectoryService(adProperties);
    }

    @Test
    void usuarioPertenceAoGrupoDeveAceitarGrupoEmFormatoCn() {
        AdUserInfo adUserInfo = new AdUserInfo(
                "Usuario Teste",
                "usuario_teste",
                "usuario_teste@ad.local",
                List.of("G.stic.Infraestrutura", "G.outro.grupo")
        );

        boolean pertence = activeDirectoryService.usuarioPertenceAoGrupo(
                adUserInfo,
                "CN=G.stic.Infraestrutura,OU=Groups,DC=tjpi,DC=local"
        );

        assertTrue(pertence);
    }

    @Test
    void usuarioPertenceAoGrupoDeveRetornarFalseParaGrupoEmBranco() {
        AdUserInfo adUserInfo = new AdUserInfo(
                "Usuario Teste",
                "usuario_teste",
                "usuario_teste@ad.local",
                List.of("G.stic.Infraestrutura")
        );

        boolean pertence = activeDirectoryService.usuarioPertenceAoGrupo(adUserInfo, "   ");

        assertFalse(pertence);
    }

    @Test
    void usuarioPertenceAoGrupoDeveRetornarFalseQuandoNaoEncontrarGrupo() {
        AdUserInfo adUserInfo = new AdUserInfo(
                "Usuario Teste",
                "usuario_teste",
                "usuario_teste@ad.local",
                List.of("G.stic.Infraestrutura")
        );

        boolean pertence = activeDirectoryService.usuarioPertenceAoGrupo(adUserInfo, "G.inexistente");

        assertFalse(pertence);
    }
}
