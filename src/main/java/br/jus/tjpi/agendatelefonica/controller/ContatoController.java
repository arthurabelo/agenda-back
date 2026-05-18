package br.jus.tjpi.agendatelefonica.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import br.jus.tjpi.agendatelefonica.model.Contato;
import br.jus.tjpi.agendatelefonica.repository.ContatoRepository;
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

    @GetMapping("/contatos")
    public ResponseEntity<List<Contato>> getContatos() {
        return ResponseEntity.ok(repository.findAll()); // Returns a list of all contacts
    }

    @GetMapping("/contatos/{id}")
    public ResponseEntity<Contato> getContatoById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok) // If found, return the contact with 200 OK
                .orElse(ResponseEntity.notFound().build()); // If not found, return 404 Not Found
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/contatos")
    public ResponseEntity<Contato> createContato(@RequestBody Contato contato) {
        contato.setId(null);
        Contato savedContato = repository.save(contato); // Saves the new contact to the database
        return ResponseEntity.ok(savedContato); // Returns the saved contact with 200 OK
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/contatos/{id}")
    public ResponseEntity<Contato> updateContato(@PathVariable Long id, @RequestBody Contato contato) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setUnidade(contato.getUnidade());
                    existing.setSetor(contato.getSetor());
                    existing.setTelefone(contato.getTelefone());
                    existing.setEndereco(contato.getEndereco());
                    existing.setLocalidade(contato.getLocalidade());
                    existing.setComarca(contato.getComarca());
                    
                    // Ajuste aqui: Sai os dois sets de boolean e entra o set de String
                    existing.setMeioDeContato(contato.getMeioDeContato());
                    existing.setTipoContato(contato.getTipoContato());
                    
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/contatos/{id}")
    public ResponseEntity<Object> deleteContato(@PathVariable Long id) {
        return repository.findById(id)
                .map(existingContato -> {
                    repository.delete(existingContato); // Deletes the contact from the database
                    return ResponseEntity.noContent().build(); // Returns 204 No Content
                })
                .orElse(ResponseEntity.notFound().build()); // If not found, return 404 Not Found
    }

    @GetMapping("/contatos/search/setor")
    public ResponseEntity<List<Contato>> searchContatosBySetor(@RequestParam String setor) {
        List<Contato> contatos = repository.findBySetorContainingIgnoreCase(setor); // Searches for contacts by setor
        return ResponseEntity.ok(contatos); // Returns the list of matching contacts with 200 OK
    }

    @GetMapping("/contatos/search")
    public ResponseEntity<Page<Contato>> searchContatos(
            @RequestParam(required = false) String termo, Pageable pageable) {
        Page<Contato> contatos = repository.findByFiltroGlobal(termo, pageable);
        return ResponseEntity.ok(contatos);
    }
}
