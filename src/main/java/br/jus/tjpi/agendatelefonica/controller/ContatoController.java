package br.jus.tjpi.agendatelefonica.controller;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors; // <-- IMPORTANTE
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import br.jus.tjpi.agendatelefonica.model.Contato;
import br.jus.tjpi.agendatelefonica.repository.ContatoRepository;
import br.jus.tjpi.agendatelefonica.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ContatoController {

    @Autowired
    private ContatoRepository repository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/contatos")
    public ResponseEntity<List<Contato>> getContatos(HttpServletRequest request) {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/contatos/{id}")
    public ResponseEntity<Contato> getContatoById(@PathVariable Long id, HttpServletRequest request) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('admin')")
    @PostMapping("/contatos")
    public ResponseEntity<Contato> createContato(@RequestBody Contato contato, HttpServletRequest request) {
        contato.setId(null);
        Contato savedContato = repository.save(contato);
        auditLogService.mark(request, "CREATE", "CONTATO", savedContato.getId(), "Contato criado: " + savedContato.getUnidade());
        auditLogService.markDetails(request, Map.of("after", snapshot(savedContato)));
        return ResponseEntity.ok(savedContato);
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
                    existing.setMeioDeContato(contato.getMeioDeContato());
                    existing.setTipoContato(contato.getTipoContato());

                    Contato updated = repository.save(existing);
                    auditLogService.mark(request, "UPDATE", "CONTATO", updated.getId(), "Contato updated: " + updated.getUnidade());
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
                    repository.delete(existingContato);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/contatos/search/setor")
    public ResponseEntity<List<Contato>> searchContatosBySetor(@RequestParam String setor, HttpServletRequest request) {
        List<Contato> contatos = repository.findBySetorContainingIgnoreCase(setor);
        return ResponseEntity.ok(contatos);
    }

    @GetMapping("/contatos/search")
    public ResponseEntity<Page<Contato>> searchContatos(
            @RequestParam(required = false) String termo, Pageable pageable, HttpServletRequest request) {
        Page<Contato> contatos = repository.findByFiltroGlobal(termo, pageable);
        return ResponseEntity.ok(contatos);
    }

    @GetMapping("/contatos/filtros-ativos")
    public ResponseEntity<Map<String, List<String>>> getFiltrosAtivos() {
        List<Object[]> resultados = repository.findComarcasEUnidades();
        Map<String, List<String>> mapaFiltros = new LinkedHashMap<>();
        for (Object[] linha : resultados) {
            String comarca = (String) linha[0];
            String unidade = (String) linha[1];
            mapaFiltros.computeIfAbsent(comarca, k -> new ArrayList<>()).add(unidade);
        }
        return ResponseEntity.ok(mapaFiltros);
    }

    public static class FiltroRelatorioDTO {
        private List<String> comarcas;
        private List<String> unidades;
        private List<String> meiosDeContato;
        private List<String> tiposContato;

        public List<String> getComarcas() { return comarcas; }
        public void setComarcas(List<String> comarcas) { this.comarcas = comarcas; }
        public List<String> getUnidades() { return unidades; }
        public void setUnidades(List<String> unidades) { this.unidades = unidades; }
        public List<String> getMeiosDeContato() { return meiosDeContato; }
        public void setMeiosDeContato(List<String> meiosDeContato) { this.meiosDeContato = meiosDeContato; }
        public List<String> getTiposContato() { return tiposContato; }
        public void setTiposContato(List<String> tiposContato) { this.tiposContato = tiposContato; }
    }

    @PreAuthorize("hasAuthority('admin')")
    @PostMapping("/contatos/relatorio/exportar")
    public void exportarExcel(
            @RequestBody FiltroRelatorioDTO filtro,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        auditLogService.mark(request, "EXPORT", "CONTATO", null, "Exportacao de relatorio em planilha Excel (.xlsx)");

        // Garante que toda a consulta seja resolvida em UPPERCASE, protegendo a query
        List<String> listaComarcas = Arrays.asList("");
        if (filtro.getComarcas() != null && !filtro.getComarcas().isEmpty()) {
            listaComarcas = filtro.getComarcas().stream().map(String::toUpperCase).collect(Collectors.toList());
        }

        List<String> listaUnidades = Arrays.asList("");
        if (filtro.getUnidades() != null && !filtro.getUnidades().isEmpty()) {
            listaUnidades = filtro.getUnidades().stream().map(String::toUpperCase).collect(Collectors.toList());
        }

        List<String> listaMeios = Arrays.asList("");
        if (filtro.getMeiosDeContato() != null && !filtro.getMeiosDeContato().isEmpty()) {
            listaMeios = filtro.getMeiosDeContato().stream().map(String::toUpperCase).collect(Collectors.toList());
        }

        List<String> listaTipos = Arrays.asList("");
        if (filtro.getTiposContato() != null && !filtro.getTiposContato().isEmpty()) {
            listaTipos = filtro.getTiposContato().stream().map(String::toUpperCase).collect(Collectors.toList());
        }

        List<Contato> contatos = repository.findContatosParaRelatorio(
                listaComarcas, listaMeios, listaTipos, listaUnidades
        );

        if (contatos.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            response.getWriter().write("Nenhum registro encontrado com os filtros informados.");
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Contatos Filtrados");
            Row headerRow = sheet.createRow(0);
            String[] colunas = {"ID", "UNIDADE", "SETOR", "COMARCA", "ENDEREÇO", "LOCALIDADE", "MEIO DE CONTATO", "TIPO DE CONTATO", "TELEFONE"};

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < colunas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(colunas[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Contato c : contatos) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(c.getId());
                row.createCell(1).setCellValue(c.getUnidade());
                row.createCell(2).setCellValue(c.getSetor() != null ? c.getSetor() : "");
                row.createCell(3).setCellValue(c.getComarca());
                row.createCell(4).setCellValue(c.getEndereco() != null ? c.getEndereco() : "");
                row.createCell(5).setCellValue(c.getLocalidade() != null ? c.getLocalidade() : "");
                row.createCell(6).setCellValue(c.getMeioDeContato() != null ? c.getMeioDeContato() : "");
                row.createCell(7).setCellValue(c.getTipoContato() != null ? c.getTipoContato() : "");
                row.createCell(8).setCellValue(c.getTelefone() != null ? c.getTelefone() : "");
            }

            for (int i = 0; i < colunas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=relatorio_agenda_contatos.xlsx");

            workbook.write(response.getOutputStream());
        }
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