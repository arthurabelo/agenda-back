package br.jus.tjpi.agendatelefonica.controller;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import br.jus.tjpi.agendatelefonica.model.Contato;
import br.jus.tjpi.agendatelefonica.repository.ContatoRepository;
import br.jus.tjpi.agendatelefonica.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api")
public class ContatoController {
    @Autowired // Injects the ContatoRepository dependency
    private ContatoRepository repository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/contatos")
    public ResponseEntity<List<Contato>> getContatos(HttpServletRequest request) {
        //auditLogService.mark(request, "LIST", "CONTATO", null, "Listagem de contatos");
        return ResponseEntity.ok(repository.findAll()); // Returns a list of all contacts
    }

    @GetMapping("/contatos/{id}")
    public ResponseEntity<Contato> getContatoById(@PathVariable Long id, HttpServletRequest request) {
        //auditLogService.mark(request, "VIEW", "CONTATO", id, "Consulta de contato por ID");
        return repository.findById(id)
                .map(ResponseEntity::ok) // If found, return the contact with 200 OK
                .orElse(ResponseEntity.notFound().build()); // If not found, return 404 Not Found
    }

    @PreAuthorize("hasAuthority('admin')")
    @PostMapping("/contatos")
    public ResponseEntity<Contato> createContato(@RequestBody Contato contato, HttpServletRequest request) {
        contato.setId(null);
        Contato savedContato = repository.save(contato); // Saves the new contact to the database
        auditLogService.mark(request, "CREATE", "CONTATO", savedContato.getId(), "Contato criado: " + savedContato.getUnidade());
        auditLogService.markDetails(request, Map.of("after", snapshot(savedContato)));
        return ResponseEntity.ok(savedContato); // Returns the saved contact with 200 OK
    }

    @PreAuthorize("hasAuthority('admin')")
    @PutMapping("/contatos/{id}")
    public ResponseEntity<Contato> updateContato(@PathVariable Long id, @RequestBody Contato contato, HttpServletRequest request) {
        auditLogService.mark(request, "UPDATE", "CONTATO", id, "Atualizacao de contato");
        return repository.findById(id)
                .map(existing -> {
                    Map<String, Object> before = snapshot(existing);
                    existing.setUnidade(contato.getUnidade());
                    existing.setSetor(contato.getSetor());
                    existing.setTelefone(contato.getTelefone());
                    existing.setEndereco(contato.getEndereco());
                    existing.setLocalidade(contato.getLocalidade());
                    existing.setComarca(contato.getComarca());

                    // Ajuste aqui: Sai os dois sets de boolean e entra o set de String
                    existing.setMeioDeContato(contato.getMeioDeContato());
                    existing.setTipoContato(contato.getTipoContato());

                    Contato updated = repository.save(existing);
                    auditLogService.mark(request, "UPDATE", "CONTATO", updated.getId(), "Contato atualizado: " + updated.getUnidade());
                    auditLogService.markDetails(request, Map.of("before", before, "after", snapshot(updated)));
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('admin')")
    @DeleteMapping("/contatos/{id}")
    public ResponseEntity<Object> deleteContato(@PathVariable Long id, HttpServletRequest request) {
        auditLogService.mark(request, "DELETE", "CONTATO", id, "Exclusao de contato");
        return repository.findById(id)
                .map(existingContato -> {
                    auditLogService.mark(request, "DELETE", "CONTATO", existingContato.getId(), "Contato excluido: " + existingContato.getUnidade());
                    auditLogService.markDetails(request, Map.of("before", snapshot(existingContato)));
                    repository.delete(existingContato); // Deletes the contact from the database
                    return ResponseEntity.noContent().build(); // Returns 204 No Content
                })
                .orElse(ResponseEntity.notFound().build()); // If not found, return 404 Not Found
    }

    @GetMapping("/contatos/search/setor")
    public ResponseEntity<List<Contato>> searchContatosBySetor(@RequestParam String setor, HttpServletRequest request) {
        //auditLogService.mark(request, "SEARCH", "CONTATO", null, "Busca de contatos por setor");
        List<Contato> contatos = repository.findBySetorContainingIgnoreCase(setor); // Searches for contacts by setor
        return ResponseEntity.ok(contatos); // Returns the list of matching contacts with 200 OK
    }

    @GetMapping("/contatos/search")
    public ResponseEntity<Page<Contato>> searchContatos(
            @RequestParam(required = false) String termo, Pageable pageable, HttpServletRequest request) {
        //auditLogService.mark(request, "SEARCH", "CONTATO", null, "Busca global de contatos");
        Page<Contato> contatos = repository.findByFiltroGlobal(termo, pageable);
        return ResponseEntity.ok(contatos);
    }

    private Map<String, Object> snapshot(Contato contato) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", contato.getId());
        data.put("unidade", contato.getUnidade());
        data.put("setor", contato.getSetor());
        data.put("comarca", contato.getComarca());
        data.put("endereco", contato.getEndereco());
        data.put("localidade", contato.getLocalidade());
        data.put("telefone", contato.getTelefone());
        data.put("meioDeContato", contato.getMeioDeContato());
        data.put("tipoContato", contato.getTipoContato());
        return data;
    }
}
