package com.oih.idp.session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdpSsoSessionRepository extends JpaRepository<IdpSsoSession, UUID> {

    Optional<IdpSsoSession> findBySessionIndex(String sessionIndex);

    Optional<IdpSsoSession> findByHttpSessionId(String httpSessionId);

    List<IdpSsoSession> findByUserIdAndRevokedAtIsNull(UUID userId);
}
