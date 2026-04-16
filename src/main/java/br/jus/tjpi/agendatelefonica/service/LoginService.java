package br.jus.tjpi.agendatelefonica.service;

import br.jus.tjpi.agendatelefonica.config.AdProperties;
import br.jus.tjpi.agendatelefonica.dto.AdUserInfo;
import br.jus.tjpi.agendatelefonica.exception.AuthConfigurationException;
import br.jus.tjpi.agendatelefonica.exception.InvalidCredentialsException;
import br.jus.tjpi.agendatelefonica.exception.InvalidPasswordException;
import br.jus.tjpi.agendatelefonica.exception.UserNotInGroupException;
import br.jus.tjpi.agendatelefonica.model.Usuario;
import br.jus.tjpi.agendatelefonica.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoginService {

    private static final String DEFAULT_AD_ROLE = "USER";

    private final UsuarioRepository usuarioRepository;
    private final ActiveDirectoryService activeDirectoryService;
    private final AdProperties adProperties;

    public LoginService(UsuarioRepository usuarioRepository,
                        ActiveDirectoryService activeDirectoryService,
                        AdProperties adProperties) {
        this.usuarioRepository = usuarioRepository;
        this.activeDirectoryService = activeDirectoryService;
        this.adProperties = adProperties;
    }

    public Usuario autenticarOuFalhar(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new InvalidCredentialsException("Usuário e senha são obrigatórios.");
        }

        String normalizedUsername = username.trim();

        Optional<Usuario> localUser = usuarioRepository.findFirstByUsernameIgnoreCase(normalizedUsername);
        if (localUser.isPresent()) {
            if (!password.equals(localUser.get().getPassword())) {
                throw new InvalidPasswordException("Senha incorreta.");
            }
            return localUser.get();
        }

        String loginGroup = adProperties.getLoginGroup();
        if (loginGroup == null || loginGroup.isBlank()) {
            throw new AuthConfigurationException("Configuração inválida: AD_LOGIN_GROUP não definido.");
        }

        validateAdConfiguration();

        Optional<AdUserInfo> adUserInfo = activeDirectoryService.autenticarEObterUsuarioAd(normalizedUsername, password);
        if (adUserInfo.isEmpty()) {
            throw new InvalidCredentialsException("Usuário não encontrado no banco e credenciais do AD inválidas.");
        }

        if (!activeDirectoryService.usuarioPertenceAoGrupo(adUserInfo.get(), loginGroup)) {
            throw new UserNotInGroupException("Usuário autenticado no AD, mas não pertence ao grupo permitido.");
        }

        Usuario adUser = new Usuario();
        adUser.setUsername(normalizedUsername);
        adUser.setRole(DEFAULT_AD_ROLE);
        adUser.setActive(true);
        return adUser;
    }

    private void validateAdConfiguration() {
        if (adProperties.getServerUrl() == null || adProperties.getServerUrl().isBlank()) {
            throw new AuthConfigurationException("Configuração inválida: AD_SERVER_URL não definido.");
        }
        if (adProperties.getBaseDn() == null || adProperties.getBaseDn().isBlank()) {
            throw new AuthConfigurationException("Configuração inválida: AD_BASE_DN não definido.");
        }
    }
}