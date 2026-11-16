package com.oih.idp.sp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "persistent_nameids")
public class PersistentNameId {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "sp_id", nullable = false)
    private UUID spId;

    @Column(name = "name_id", nullable = false)
    private String nameId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PersistentNameId() {
    }

    public PersistentNameId(UUID userId, UUID spId, String nameId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.spId = spId;
        this.nameId = nameId;
        this.createdAt = Instant.now();
    }

    public String getNameId() {
        return nameId;
    }
}
