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


    // Query do Relatório com filtros condicionais (Se o parâmetro for nulo, o Spring ignora ele)
    @Query("SELECT DISTINCT LOWER(c.comarca) FROM Contato c WHERE c.comarca IS NOT NULL ORDER BY LOWER(c.comarca) ASC")
    List<String> findDistinctComarcas();

    // Retorna pares distintos [comarca, unidade] em CAIXA ALTA, filtrando nulos/vazios, ordenados asc
    @Query("SELECT DISTINCT UPPER(c.comarca), UPPER(c.unidade) FROM Contato c WHERE " +
           "(c.comarca IS NOT NULL AND TRIM(c.comarca) <> '') AND " +
           "(c.unidade IS NOT NULL AND TRIM(c.unidade) <> '') " +
           "ORDER BY UPPER(c.comarca) ASC, UPPER(c.unidade) ASC")
    List<Object[]> findComarcasEUnidades();

    // Query do Relatório com filtros multi-seleção (listas). Usa string vazia curinga para
    // evitar AST Exception do Hibernate quando a lista estiver vazia.
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
