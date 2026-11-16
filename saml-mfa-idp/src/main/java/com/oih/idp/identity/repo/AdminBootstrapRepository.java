package com.oih.idp.identity.repo;

import com.oih.idp.identity.domain.AdminBootstrap;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminBootstrapRepository extends JpaRepository<AdminBootstrap, UUID> {

    Optional<AdminBootstrap> findFirstByConsumedAtIsNullOrderByCreatedAtAsc();

    boolean existsByConsumedAtIsNull();
}
