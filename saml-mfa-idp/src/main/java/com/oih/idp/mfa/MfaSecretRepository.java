package com.oih.idp.mfa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MfaSecretRepository extends JpaRepository<MfaSecret, UUID> {
}
