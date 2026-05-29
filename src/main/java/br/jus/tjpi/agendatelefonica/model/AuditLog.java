package br.jus.tjpi.agendatelefonica.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
                @Index(name = "idx_audit_actor", columnList = "actor"),
                @Index(name = "idx_audit_action", columnList = "action"),
                @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id")
        }
)
@Data
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    @Column(length = 100)
    private String actor;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(length = 500)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(length = 10)
    private String method;

    @Column(length = 500)
    private String uri;
}
