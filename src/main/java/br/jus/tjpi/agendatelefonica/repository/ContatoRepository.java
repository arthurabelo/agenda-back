package br.jus.tjpi.agendatelefonica.repository;

import br.jus.tjpi.agendatelefonica.model.Contato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ContatoRepository  extends JpaRepository<Contato, Long> {
    
    List<Contato> findBySetorContainingIgnoreCase(String setor);
    
    List<Contato> findByTelefoneContainingIgnoreCase(String telefone);
    
    List<Contato> findByLocalContainingIgnoreCase(String local);
    
    @Query("SELECT c FROM Contato c WHERE " +
           "(:telefone IS NULL OR LOWER(c.telefone) LIKE LOWER(CONCAT('%', :telefone, '%'))) AND " +
           "(:local IS NULL OR LOWER(c.local) LIKE LOWER(CONCAT('%', :local, '%')))")
    List<Contato> findByTelefoneAndLocalOptional(@Param("telefone") String telefone, @Param("local") String local);
}
