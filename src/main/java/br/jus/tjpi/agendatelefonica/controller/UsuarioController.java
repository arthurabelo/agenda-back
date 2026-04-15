package br.jus.tjpi.agendatelefonica.controller;

import br.jus.tjpi.agendatelefonica.dto.AdLoginRequest;
import br.jus.tjpi.agendatelefonica.dto.AdUserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.*;

import br.jus.tjpi.agendatelefonica.model.Usuario;
import br.jus.tjpi.agendatelefonica.repository.UsuarioRepository;
import br.jus.tjpi.agendatelefonica.service.ActiveDirectoryService;

@RestController
public class UsuarioController {
    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private ActiveDirectoryService activeDirectoryService;

    @GetMapping("/usuarios")
    public ResponseEntity<List<Usuario>> getUsuarios() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<Usuario> getUsuarioById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/usuarios")
    public ResponseEntity<Usuario> createUsuario(@RequestBody Usuario usuario) {
        usuario.setId(null);
        if (usuario.getRole() == null || usuario.getRole().isBlank()) {
            usuario.setRole("USER");
        }
        Usuario savedUsuario = repository.save(usuario);
        return ResponseEntity.ok(savedUsuario);
    }

    @PostMapping("/usuarios/ad/login")
    public ResponseEntity<?> loginUsuarioAd(@RequestBody AdLoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "O username do AD é obrigatório."
            ));
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "A senha do AD é obrigatória."
            ));
        }

        String normalizedUsername = request.getUsername().trim();
        Optional<AdUserInfo> adUserInfo = activeDirectoryService.autenticarEObterUsuarioAd(normalizedUsername, request.getPassword());

        if (adUserInfo.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "success", false,
                "message", "Usuário ou senha inválidos no Active Directory."
            ));
        }

        Optional<Usuario> usuarioDb = repository.findFirstByUsernameIgnoreCase(adUserInfo.get().sAMAccountName());
        if (usuarioDb.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "message", "Usuário autenticado no AD, mas não cadastrado no sistema."
            ));
        }

        Usuario usuario = usuarioDb.get();
        
        // Verifica se o usuário é admin
        boolean isSuperAdmin = adUserInfo.get().groups().stream()
                .anyMatch(group -> group.equalsIgnoreCase("G.stic.agendatelefonica.superadmin"));
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Usuário autenticado com sucesso.");
        response.put("user", Map.of(
            "id", usuario.getId(),
            "username", usuario.getUsername(),
            "role", usuario.getRole(),
            "active", usuario.isActive(),
            "displayName", adUserInfo.get().displayName(),
            "userPrincipalName", adUserInfo.get().userPrincipalName(),
            "isSuperAdmin", isSuperAdmin,
            "groups", adUserInfo.get().groups()
        ));

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/usuarios/delete/{id}")
    public ResponseEntity<Object> deleteUsuario(@PathVariable Long id) {
        return repository.findById(id)
                .map(existingUsuario -> {
                    repository.delete(existingUsuario);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
    

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<Usuario> updateUsuario(@PathVariable Long id, @RequestBody Usuario usuario) {
        return repository.findById(id)
                .map(existingUsuario -> {
                    existingUsuario.setUsername(usuario.getUsername());
                    existingUsuario.setPassword(usuario.getPassword());
                    Usuario updatedUsuario = repository.save(existingUsuario);
                    return ResponseEntity.ok(updatedUsuario);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuarios/search")
    public ResponseEntity<List<Usuario>> searchUsuarios(@RequestParam String username) {
        return ResponseEntity.ok(repository.findByUsername(username));
    }

}