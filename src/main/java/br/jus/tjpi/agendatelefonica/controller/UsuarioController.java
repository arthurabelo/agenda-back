package br.jus.tjpi.agendatelefonica.controller;

import br.jus.tjpi.agendatelefonica.dto.AdRegisterRequest;
import br.jus.tjpi.agendatelefonica.dto.AdRegisterResponse;
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

    @PostMapping("/usuarios/ad")
    public ResponseEntity<?> createUsuarioFromAd(@RequestBody AdRegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "O username do AD é obrigatório."
            ));
        }

        String normalizedUsername = request.getUsername().trim();
        if (repository.findFirstByUsernameIgnoreCase(normalizedUsername).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "message", "Usuário já cadastrado."
            ));
        }

        Optional<AdUserInfo> adUserInfo;
        try {
            adUserInfo = activeDirectoryService.buscarUsuarioPorUsername(normalizedUsername);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
            ));
        }

        if (adUserInfo.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "Usuário não encontrado no Active Directory."
            ));
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(adUserInfo.get().sAMAccountName());
        usuario.setPassword("{AD}");
        usuario.setRole((request.getRole() == null || request.getRole().isBlank()) ? "USER" : request.getRole().trim());
        usuario.setActive(request.getActive() == null || request.getActive());

        Usuario savedUsuario = repository.save(usuario);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Usuário registrado com sucesso via AD.");
        response.put("user", new AdRegisterResponse(
                savedUsuario.getId(),
                savedUsuario.getUsername(),
                adUserInfo.get().displayName(),
                adUserInfo.get().userPrincipalName(),
                savedUsuario.getRole(),
                savedUsuario.isActive()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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