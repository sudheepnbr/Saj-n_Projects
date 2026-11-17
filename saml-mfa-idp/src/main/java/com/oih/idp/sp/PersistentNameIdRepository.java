package com.oih.idp.sp;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersistentNameIdRepository extends JpaRepository<PersistentNameId, UUID> {

    Optional<PersistentNameId> findByUserIdAndSpId(UUID userId, UUID spId);
}
