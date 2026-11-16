package com.oih.idp.keys;

import com.oih.idp.keys.SigningKey.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SigningKeyRepository extends JpaRepository<SigningKey, UUID> {

    Optional<SigningKey> findByStatus(Status status);

    List<SigningKey> findByStatusIn(List<Status> statuses);
}
