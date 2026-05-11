package br.jus.tjpi.agendatelefonica.repository;

import br.jus.tjpi.agendatelefonica.model.Contato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ContatoRepository  extends JpaRepository<Contato, Long> {
    
    List<Contato> findByUnidadeContainingIgnoreCase(String unidade);

    List<Contato> findBySetorContainingIgnoreCase(String setor);

    List<Contato> findByComarcaContainingIgnoreCase(String comarca);
    
    List<Contato> findByLocalidadeContainingIgnoreCase(String localidade);

    List<Contato> findByTelefoneContainingIgnoreCase(String telefone);
    
    @Query("SELECT c FROM Contato c WHERE " +
           "(:termo IS NULL OR (" +
           "LOWER(c.unidade) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.setor) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.localidade) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.comarca) LIKE LOWER(CONCAT('%', :termo, '%')) OR " + // Adicionado comarca aqui também
           "LOWER(c.endereco) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.telefone) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.tipoContato) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.meioDeContato) LIKE LOWER(CONCAT('%', :termo, '%'))")
    List<Contato> findByFiltroGlobal(
        @Param("termo") String termo
    );
}
