package com.oih.idp.sp;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpSessionRepository extends JpaRepository<SpSession, UUID> {

    List<SpSession> findByIdpSessionId(UUID idpSessionId);

    List<SpSession> findBySessionIndex(String sessionIndex);
}
