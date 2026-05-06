package br.jus.tjpi.agendatelefonica.repository;

import br.jus.tjpi.agendatelefonica.model.Contato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ContatoRepository extends JpaRepository<Contato, Long> {
    
    List<Contato> findBySetorContainingIgnoreCase(String setor);
    
    @Query("SELECT c FROM Contato c WHERE " +
           "LOWER(c.unidade) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.setor) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.localidade) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.comarca) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.endereco) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.telefone) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.tipoContato) LIKE LOWER(CONCAT('%', :termo, '%'))")
    List<Contato> findByFiltroGlobal(@Param("termo") String termo);
}
