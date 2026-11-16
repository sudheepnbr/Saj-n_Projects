package com.oih.idp.sp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "attribute_mappings")
public class AttributeMapping {

    public static final Set<String> BLOCKED = Set.of(
            "passwordHash", "totpCiphertext", "lockUntil", "failedLogins", "password_hash");

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sp_id")
    private ServiceProvider serviceProvider;

    @Column(name = "saml_name", nullable = false)
    private String samlName;

    @Column(name = "source_field", nullable = false)
    private String sourceField;

    protected AttributeMapping() {
    }

    public AttributeMapping(ServiceProvider sp, String samlName, String sourceField) {
        if (BLOCKED.contains(sourceField)) {
            throw new IllegalArgumentException("Blocked source field");
        }
        this.id = UUID.randomUUID();
        this.serviceProvider = sp;
        this.samlName = samlName;
        this.sourceField = sourceField;
    }

    public String getSamlName() {
        return samlName;
    }

    public String getSourceField() {
        return sourceField;
    }
}
