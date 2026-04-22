package br.jus.tjpi.agendatelefonica.repository;
import br.jus.tjpi.agendatelefonica.model.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    List<Usuario> findByUsername(String username);
    Optional<Usuario> findFirstByUsernameIgnoreCase(String username);
}
