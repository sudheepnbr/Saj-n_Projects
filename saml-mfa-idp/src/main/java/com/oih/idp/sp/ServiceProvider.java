package com.oih.idp.sp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "service_providers")
public class ServiceProvider {

    @Id
    private UUID id;

    @Column(name = "entity_id", nullable = false, unique = true)
    private String entityId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "want_authn_requests_signed", nullable = false)
    private boolean wantAuthnRequestsSigned = true;

    @Column(name = "encrypt_assertions", nullable = false)
    private boolean encryptAssertions = false;

    @Column(name = "nameid_format", nullable = false)
    private String nameIdFormat = "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent";

    @Column(name = "mfa_required", nullable = false)
    private boolean mfaRequired = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt;

    @OneToMany(mappedBy = "serviceProvider", cascade = CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    private List<SpAcs> acs = new ArrayList<>();

    @OneToMany(mappedBy = "serviceProvider", cascade = CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    private List<SpSlo> slo = new ArrayList<>();

    @OneToMany(mappedBy = "serviceProvider", cascade = CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    private List<SpCertificate> certificates = new ArrayList<>();

    @OneToMany(mappedBy = "serviceProvider", cascade = CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    private List<AttributeMapping> mappings = new ArrayList<>();

    protected ServiceProvider() {
    }

    public static ServiceProvider create(String entityId, String displayName) {
        ServiceProvider sp = new ServiceProvider();
        sp.id = UUID.randomUUID();
        sp.entityId = entityId;
        sp.displayName = displayName;
        Instant now = Instant.now();
        sp.createdAt = now;
        sp.modifiedAt = now;
        return sp;
    }

    public UUID getId() {
        return id;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isWantAuthnRequestsSigned() {
        return wantAuthnRequestsSigned;
    }

    public void setWantAuthnRequestsSigned(boolean wantAuthnRequestsSigned) {
        this.wantAuthnRequestsSigned = wantAuthnRequestsSigned;
    }

    public boolean isEncryptAssertions() {
        return encryptAssertions;
    }

    public void setEncryptAssertions(boolean encryptAssertions) {
        this.encryptAssertions = encryptAssertions;
    }

    public String getNameIdFormat() {
        return nameIdFormat;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public List<SpAcs> getAcs() {
        return acs;
    }

    public List<SpSlo> getSlo() {
        return slo;
    }

    public List<SpCertificate> getCertificates() {
        return certificates;
    }

    public List<AttributeMapping> getMappings() {
        return mappings;
    }

    public boolean allowsAcs(String url, String binding) {
        return acs.stream().anyMatch(entry -> entry.getUrl().equals(url) && entry.getBinding().equals(binding));
    }

    public String defaultAcs(String binding) {
        return acs.stream()
                .filter(entry -> entry.getBinding().equals(binding) && entry.isDefaultAcs())
                .map(SpAcs::getUrl)
                .findFirst()
                .orElseGet(() -> acs.stream()
                        .filter(entry -> entry.getBinding().equals(binding))
                        .map(SpAcs::getUrl)
                        .findFirst()
                        .orElse(null));
    }
}
