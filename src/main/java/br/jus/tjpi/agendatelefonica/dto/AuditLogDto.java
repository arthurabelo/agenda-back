package br.jus.tjpi.agendatelefonica.dto;

import br.jus.tjpi.agendatelefonica.model.AuditLog;

import java.time.Instant;
import java.util.Map;

public record AuditLogDto(
        Long id,
        Instant timestamp,
        String actor,
        String action,
        String entityType,
        Long entityId,
        String description,
        Map<String, Object> details,
        String ipAddress,
        String method,
        String uri
) {
    public static AuditLogDto from(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getTimestamp(),
                log.getActor(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDescription(),
                log.getDetails(),
                log.getIpAddress(),
                log.getMethod(),
                log.getUri()
        );
    }
}
