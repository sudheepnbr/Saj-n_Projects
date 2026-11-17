package com.oih.idp.sp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sp_sessions")
public class SpSession {

    @Id
    private UUID id;

    @Column(name = "idp_session_id", nullable = false)
    private UUID idpSessionId;

    @Column(name = "sp_id", nullable = false)
    private UUID spId;

    @Column(name = "session_index", nullable = false)
    private String sessionIndex;

    @Column(name = "name_id", nullable = false)
    private String nameId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "slo_status", nullable = false)
    private String sloStatus = "ACTIVE";

    protected SpSession() {
    }

    public SpSession(UUID idpSessionId, UUID spId, String sessionIndex, String nameId) {
        this.id = UUID.randomUUID();
        this.idpSessionId = idpSessionId;
        this.spId = spId;
        this.sessionIndex = sessionIndex;
        this.nameId = nameId;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdpSessionId() {
        return idpSessionId;
    }

    public UUID getSpId() {
        return spId;
    }

    public String getSessionIndex() {
        return sessionIndex;
    }

    public String getSloStatus() {
        return sloStatus;
    }

    public void setSloStatus(String sloStatus) {
        this.sloStatus = sloStatus;
    }
}
