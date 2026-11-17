package com.oih.idp.sp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "sp_slo")
public class SpSlo {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sp_id")
    private ServiceProvider serviceProvider;

    @Column(nullable = false)
    private String binding;

    @Column(nullable = false)
    private String url;

    protected SpSlo() {
    }

    public SpSlo(ServiceProvider sp, String binding, String url) {
        this.id = UUID.randomUUID();
        this.serviceProvider = sp;
        this.binding = binding;
        this.url = url;
    }

    public String getBinding() {
        return binding;
    }

    public String getUrl() {
        return url;
    }
}
