package com.oih.idp.identity.repo;

import com.oih.idp.identity.domain.AuthSourceThrottle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSourceThrottleRepository extends JpaRepository<AuthSourceThrottle, String> {
}
