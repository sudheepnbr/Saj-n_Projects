package com.oih.idp.identity.repo;

import com.oih.idp.identity.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameNormalized(String usernameNormalized);

    Optional<User> findByEmailNormalized(String emailNormalized);

    boolean existsByUsernameNormalized(String usernameNormalized);

    boolean existsByEmailNormalized(String emailNormalized);

    long count();
}
