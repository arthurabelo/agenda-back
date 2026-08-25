package br.jus.tjpi.agendatelefonica.repository;

import br.jus.tjpi.agendatelefonica.model.Contato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
       "LOWER(c.comarca) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
       "LOWER(c.endereco) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
       "LOWER(c.telefone) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
       "LOWER(c.tipoContato) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
       "LOWER(c.meioDeContato) LIKE LOWER(CONCAT('%', :termo, '%'))" +
       "))"
    )
    Page<Contato> findByFiltroGlobal(@Param("termo") String termo, Pageable pageable);
    
    // Busca somente os valores necessários para os filtros. A normalização também
    // elimina espaços acidentais e evita que variações de maiúsculas gerem duplicatas.
    @Query("SELECT DISTINCT UPPER(TRIM(c.comarca)), UPPER(TRIM(c.unidade)) FROM Contato c " +
            "WHERE c.comarca IS NOT NULL AND TRIM(c.comarca) <> '' " +
            "AND c.unidade IS NOT NULL AND TRIM(c.unidade) <> '' " +
            "ORDER BY UPPER(TRIM(c.comarca)), UPPER(TRIM(c.unidade))")
    List<Object[]> findComarcasEUnidades();

    @Query("SELECT c FROM Contato c WHERE " +
            "('' IN :comarcas OR UPPER(c.comarca) IN :comarcas) AND " +
            "('' IN :meiosDeContato OR UPPER(c.meioDeContato) IN :meiosDeContato) AND " +
            "('' IN :tiposContato OR UPPER(c.tipoContato) IN :tiposContato) AND " +
            "('' IN :unidades OR UPPER(c.unidade) IN :unidades)")
    List<Contato> findContatosParaRelatorio(
            @Param("comarcas") List<String> comarcas,
            @Param("meiosDeContato") List<String> meiosDeContato,
            @Param("tiposContato") List<String> tiposContato,
            @Param("unidades") List<String> unidades
    );
}
