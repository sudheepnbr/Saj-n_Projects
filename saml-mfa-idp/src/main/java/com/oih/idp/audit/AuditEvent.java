package com.oih.idp.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "sp_entity_id")
    private String spEntityId;

    @Column(nullable = false, length = 32)
    private String outcome;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "client_ip")
    private InetAddress clientIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "failure_category", length = 64)
    private String failureCategory;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String details = "{}";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    public static AuditEvent of(
            String eventType,
            String outcome,
            UUID actorUserId,
            InetAddress clientIp,
            String correlationId,
            String failureCategory) {
        AuditEvent event = new AuditEvent();
        event.id = UUID.randomUUID();
        event.eventType = eventType;
        event.outcome = outcome;
        event.actorUserId = actorUserId;
        event.clientIp = clientIp;
        event.correlationId = correlationId;
        event.failureCategory = failureCategory;
        event.createdAt = Instant.now();
        event.details = "{}";
        return event;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getOutcome() {
        return outcome;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getFailureCategory() {
        return failureCategory;
    }

    public InetAddress getClientIp() {
        return clientIp;
    }
}
