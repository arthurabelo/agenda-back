package br.jus.tjpi.agendatelefonica.controller;

import br.jus.tjpi.agendatelefonica.dto.LoginRequest;
import br.jus.tjpi.agendatelefonica.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import br.jus.tjpi.agendatelefonica.model.Usuario;
import br.jus.tjpi.agendatelefonica.repository.UsuarioRepository;
import br.jus.tjpi.agendatelefonica.service.AuditLogService;
import br.jus.tjpi.agendatelefonica.service.LoginService;
import br.jus.tjpi.agendatelefonica.service.TokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Adicionado para facilitar integração com o Front
public class UsuarioController {
    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private LoginService loginService;

    @Autowired
    private AuditLogService auditLogService;

    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/usuarios")
    public ResponseEntity<List<Usuario>> getUsuarios(HttpServletRequest request) {
        auditLogService.mark(request, "LIST", "USUARIO", null, "Listagem de usuarios");
        return ResponseEntity.ok(repository.findAll());
    }

    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<Usuario> getUsuarioById(@PathVariable Long id, HttpServletRequest request) {
        auditLogService.mark(request, "VIEW", "USUARIO", id, "Consulta de usuario por ID");
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('admin')")
    @PostMapping("/usuarios")
    public ResponseEntity<Usuario> createUsuario(@RequestBody Usuario usuario, HttpServletRequest request) {
        usuario.setId(null);
        if (usuario.getRole() == null || usuario.getRole().isBlank()) {
            usuario.setRole("admin"); // Alterado de USER para admin
        }
        Usuario savedUsuario = repository.save(usuario);
        auditLogService.mark(request, "CREATE", "USUARIO", savedUsuario.getId(), "Usuario criado: " + savedUsuario.getUsername());
        auditLogService.markDetails(request, Map.of("after", snapshot(savedUsuario)));
        return ResponseEntity.ok(savedUsuario);
    }

    @PreAuthorize("hasAuthority('admin')")
    @DeleteMapping("/usuarios/delete/{id}")
    public ResponseEntity<Object> deleteUsuario(@PathVariable Long id, HttpServletRequest request) {
        auditLogService.mark(request, "DELETE", "USUARIO", id, "Exclusao de usuario");
        return repository.findById(id)
                .map(existingUsuario -> {
                    auditLogService.mark(request, "DELETE", "USUARIO", existingUsuario.getId(), "Usuario excluido: " + existingUsuario.getUsername());
                    auditLogService.markDetails(request, Map.of("before", snapshot(existingUsuario)));
                    repository.delete(existingUsuario);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }


    @PreAuthorize("hasAuthority('admin')")
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<Usuario> updateUsuario(@PathVariable Long id, @RequestBody Usuario usuario, HttpServletRequest request) {
        auditLogService.mark(request, "UPDATE", "USUARIO", id, "Atualizacao de usuario");
        return repository.findById(id)
                .map(existingUsuario -> {
                    Map<String, Object> before = snapshot(existingUsuario);
                    existingUsuario.setUsername(usuario.getUsername());
                    // Atualiza senha apenas se enviada
                    if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
                        existingUsuario.setPassword(usuario.getPassword());
                    }
                    // Mantém a role e status sincronizados
                    existingUsuario.setRole(usuario.getRole());
                    existingUsuario.setActive(usuario.isActive());
                    existingUsuario.setAdUser(usuario.isAdUser());

                    Usuario updatedUsuario = repository.save(existingUsuario);
                    auditLogService.mark(request, "UPDATE", "USUARIO", updatedUsuario.getId(), "Usuario atualizado: " + updatedUsuario.getUsername());
                    auditLogService.markDetails(request, Map.of("before", before, "after", snapshot(updatedUsuario)));
                    return ResponseEntity.ok(updatedUsuario);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/usuarios/search")
    public ResponseEntity<List<Usuario>> searchUsuarios(@RequestParam String username, HttpServletRequest request) {
        auditLogService.mark(request, "SEARCH", "USUARIO", null, "Busca de usuarios");
        return ResponseEntity.ok(repository.findByUsername(username));
    }

    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response, HttpServletRequest httpRequest) {
        auditLogService.mark(httpRequest, "LOGIN", "USUARIO", null, "Tentativa de login: " + request.username());
        try {
            Usuario usuario = loginService.autenticarOuFalhar(request.username(), request.password());
            auditLogService.markActor(httpRequest, usuario);
            auditLogService.mark(httpRequest, "LOGIN", "USUARIO", usuario.getId(), "Login realizado: " + usuario.getUsername());
            String token = tokenService.gerarToken(usuario);
            Cookie jwtCookie = new Cookie("jwt_token", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            response.addCookie(jwtCookie);

            LoginResponse dadosVisuais = new LoginResponse(
                    usuario.getId(),
                    usuario.getUsername(),
                    usuario.getRole(),
                    usuario.isActive()
            );

            return ResponseEntity.ok(dadosVisuais);
        } catch (RuntimeException ex) {
            auditLogService.mark(httpRequest, "LOGIN_FAILED", "USUARIO", null, "Falha de login: " + request.username());
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response, HttpServletRequest request) {
        auditLogService.mark(request, "LOGOUT", "USUARIO", null, "Logout realizado");
        Cookie jwtCookie = new Cookie("jwt_token", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);

        response.addCookie(jwtCookie);
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> snapshot(Usuario usuario) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", usuario.getId());
        data.put("username", usuario.getUsername());
        data.put("role", usuario.getRole());
        data.put("active", usuario.isActive());
        data.put("adUser", usuario.isAdUser());
        return data;
    }
}
