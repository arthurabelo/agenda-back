package br.jus.tjpi.agendatelefonica.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import br.jus.tjpi.agendatelefonica.model.Contato;
import br.jus.tjpi.agendatelefonica.repository.ContatoRepository;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ContatoController {

    @Autowired
    private ContatoRepository repository;

    @GetMapping("/contatos")
    public ResponseEntity<List<Contato>> getContatos() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping("/contatos")
    public ResponseEntity<Contato> createContato(@RequestBody Contato contato) {
        contato.setId(null);
        return ResponseEntity.ok(repository.save(contato));
    }

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
                    existing.setTipoContato(contato.getTipoContato());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/contatos/{id}")
    public ResponseEntity<Object> deleteContato(@PathVariable Long id) {
        return repository.findById(id).map(c -> {
            repository.delete(c);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/contatos/search")
    public ResponseEntity<List<Contato>> searchContatos(@RequestParam(required = false) String termo) {
        if (termo == null || termo.isBlank()) return ResponseEntity.ok(repository.findAll());
        return ResponseEntity.ok(repository.findByFiltroGlobal(termo));
    }
}
