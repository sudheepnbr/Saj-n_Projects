package com.oih.idp.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {

    public static final String USER = "USER";
    public static final String OPERATOR = "OPERATOR";
    public static final String ADMIN = "ADMIN";
    public static final String AUDITOR = "AUDITOR";

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String name;

    protected Role() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
