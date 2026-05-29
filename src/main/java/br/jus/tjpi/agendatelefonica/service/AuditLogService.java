package br.jus.tjpi.agendatelefonica.service;

import br.jus.tjpi.agendatelefonica.model.AuditLog;
import br.jus.tjpi.agendatelefonica.model.Usuario;
import br.jus.tjpi.agendatelefonica.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private static final int MAX_DETAILS_CHARS = 64 * 1024;
    private static final String ATTR_LAST_ID = "audit.lastId";
    private static final String ATTR_ACTOR = "audit.actor";

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void mark(HttpServletRequest request,
                     String action,
                     String entity,
                     Long entityId,
                     String description) {
        String actor = resolveActor(request);
        String ip = resolveClientIp(request);
        String method = request != null ? request.getMethod() : null;
        String uri = request != null ? request.getRequestURI() : null;

        log.info("audit action={} entity={} id={} actor={} ip={} method={} uri={} description=\"{}\"",
                action, entity, entityId, actor, ip, method, uri, description);

        try {
            AuditLog entry = new AuditLog();
            entry.setTimestamp(Instant.now());
            entry.setActor(actor);
            entry.setAction(action);
            entry.setEntityType(entity);
            entry.setEntityId(entityId);
            entry.setDescription(description);
            entry.setIpAddress(ip);
            entry.setMethod(method);
            entry.setUri(uri);

            AuditLog saved = repository.save(entry);
            if (request != null) {
                request.setAttribute(ATTR_LAST_ID, saved.getId());
            }
        } catch (Exception ex) {
            log.warn("audit-persist-failed action={} entity={} id={}: {}", action, entity, entityId, ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDetails(HttpServletRequest request, Map<String, Object> details) {
        if (details == null) {
            return;
        }

        Map<String, Object> safeDetails = truncateIfTooLarge(details);

        log.info("audit-details actor={} ip={} uri={} details={}",
                resolveActor(request),
                resolveClientIp(request),
                request != null ? request.getRequestURI() : null,
                safeDetails);

        try {
            Long lastId = request != null ? (Long) request.getAttribute(ATTR_LAST_ID) : null;
            if (lastId != null) {
                repository.findById(lastId).ifPresentOrElse(
                        existing -> {
                            existing.setDetails(safeDetails);
                            repository.save(existing);
                        },
                        () -> persistStandaloneDetails(request, safeDetails)
                );
            } else {
                persistStandaloneDetails(request, safeDetails);
            }
        } catch (Exception ex) {
            log.warn("audit-details-persist-failed: {}", ex.getMessage());
        }
    }

    public void markActor(HttpServletRequest request, Usuario usuario) {
        if (request == null || usuario == null) {
            return;
        }
        request.setAttribute(ATTR_ACTOR, usuario.getUsername());
    }

    private void persistStandaloneDetails(HttpServletRequest request, Map<String, Object> details) {
        AuditLog entry = new AuditLog();
        entry.setTimestamp(Instant.now());
        entry.setActor(resolveActor(request));
        entry.setAction(AuditAction.DETAILS);
        entry.setDetails(details);
        entry.setIpAddress(resolveClientIp(request));
        entry.setMethod(request != null ? request.getMethod() : null);
        entry.setUri(request != null ? request.getRequestURI() : null);
        repository.save(entry);
    }

    private Map<String, Object> truncateIfTooLarge(Map<String, Object> details) {
        if (details.toString().length() <= MAX_DETAILS_CHARS) {
            return details;
        }
        return Map.of("truncated", true, "reason", "payload-too-large");
    }

    private String resolveActor(HttpServletRequest request) {
        if (request != null) {
            Object actorAttr = request.getAttribute(ATTR_ACTOR);
            if (actorAttr != null) {
                return actorAttr.toString();
            }
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
