package com.oih.idp.sp;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, UUID> {

    Optional<ServiceProvider> findByEntityId(String entityId);
}
