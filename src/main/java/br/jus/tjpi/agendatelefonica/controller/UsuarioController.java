package br.jus.tjpi.agendatelefonica.controller;

import br.jus.tjpi.agendatelefonica.dto.LoginRequest;
import br.jus.tjpi.agendatelefonica.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import br.jus.tjpi.agendatelefonica.model.Usuario;
import br.jus.tjpi.agendatelefonica.repository.UsuarioRepository;
import br.jus.tjpi.agendatelefonica.service.LoginService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Adicionado para facilitar integração com o Front
public class UsuarioController {
    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private LoginService loginService;

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
            usuario.setRole("admin"); // Alterado de USER para admin
        }
        Usuario savedUsuario = repository.save(usuario);
        return ResponseEntity.ok(savedUsuario);
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
                    // Atualiza senha apenas se enviada
                    if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
                        existingUsuario.setPassword(usuario.getPassword());
                    }
                    // Mantém a role e status sincronizados
                    existingUsuario.setRole(usuario.getRole());
                    existingUsuario.setActive(usuario.isActive());
                    existingUsuario.setAdUser(usuario.isAdUser());
                    
                    Usuario updatedUsuario = repository.save(existingUsuario);
                    return ResponseEntity.ok(updatedUsuario);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuarios/search")
    public ResponseEntity<List<Usuario>> searchUsuarios(@RequestParam String username) {
        return ResponseEntity.ok(repository.findByUsername(username));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Usuario usuario = loginService.autenticarOuFalhar(request.username(), request.password());
        LoginResponse response = new LoginResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getRole(),
                usuario.isActive()
        );
        return ResponseEntity.ok(response);
    }

}
