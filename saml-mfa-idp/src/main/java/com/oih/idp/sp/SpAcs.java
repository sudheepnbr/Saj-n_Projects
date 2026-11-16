package com.oih.idp.sp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "sp_acs")
public class SpAcs {

    public static final String POST = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";
    public static final String REDIRECT = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sp_id")
    private ServiceProvider serviceProvider;

    @Column(nullable = false)
    private String binding;

    @Column(nullable = false)
    private String url;

    @Column(name = "default_acs", nullable = false)
    private boolean defaultAcs;

    protected SpAcs() {
    }

    public SpAcs(ServiceProvider sp, String binding, String url, boolean defaultAcs) {
        this.id = UUID.randomUUID();
        this.serviceProvider = sp;
        this.binding = binding;
        this.url = url;
        this.defaultAcs = defaultAcs;
    }

    public String getBinding() {
        return binding;
    }

    public String getUrl() {
        return url;
    }

    public boolean isDefaultAcs() {
        return defaultAcs;
    }
}
