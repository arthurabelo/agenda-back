package br.jus.tjpi.agendatelefonica.controller;

import br.jus.tjpi.agendatelefonica.dto.AuditLogDto;
import br.jus.tjpi.agendatelefonica.model.AuditLog;
import br.jus.tjpi.agendatelefonica.repository.AuditLogRepository;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private static final int MAX_PAGE_SIZE = 200;

    private final AuditLogRepository repository;

    public AuditLogController(AuditLogRepository repository) {
        this.repository = repository;
    }

    @PreAuthorize("hasAuthority('admin')")
    @GetMapping
    public ResponseEntity<AuditLogPageResponse> list(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Pageable effective = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        Specification<AuditLog> spec = buildSpec(
                nullIfBlank(actor),
                nullIfBlank(action),
                nullIfBlank(entityType),
                nullIfBlank(termo),
                from,
                to
        );

        Page<AuditLogDto> page = repository.findAll(spec, effective).map(AuditLogDto::from);
        return ResponseEntity.ok(AuditLogPageResponse.from(page));
    }

    private String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private Specification<AuditLog> buildSpec(String actor,
                                              String action,
                                              String entityType,
                                              String termo,
                                              Instant from,
                                              Instant to) {
        List<Specification<AuditLog>> specs = new ArrayList<>();
        addIfPresent(specs, containsIgnoreCase("actor", actor));
        addIfPresent(specs, equalsIgnoreCase("action", action));
        addIfPresent(specs, equalsIgnoreCase("entityType", entityType));
        addIfPresent(specs, containsIgnoreCase("description", termo));
        addIfPresent(specs, from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from));
        addIfPresent(specs, to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), to));

        return specs.stream()
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    private void addIfPresent(List<Specification<AuditLog>> specs, Specification<AuditLog> spec) {
        if (spec != null) {
            specs.add(spec);
        }
    }

    private Specification<AuditLog> containsIgnoreCase(String field, String value) {
        if (value == null) {
            return null;
        }
        String pattern = "%" + value.toLowerCase() + "%";
        return (root, query, cb) -> {
            Expression<String> safeField = cb.coalesce(root.get(field), "");
            return cb.like(cb.lower(safeField), pattern);
        };
    }

    private Specification<AuditLog> equalsIgnoreCase(String field, String value) {
        if (value == null) {
            return null;
        }
        String expected = value.toLowerCase();
        return (root, query, cb) -> {
            Expression<String> safeField = cb.coalesce(root.get(field), "");
            return cb.equal(cb.lower(safeField), expected);
        };
    }

    public record AuditLogPageResponse(
            List<AuditLogDto> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
        static AuditLogPageResponse from(Page<AuditLogDto> page) {
            return new AuditLogPageResponse(
                    page.getContent(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.getNumber(),
                    page.getSize()
            );
        }
    }
}
