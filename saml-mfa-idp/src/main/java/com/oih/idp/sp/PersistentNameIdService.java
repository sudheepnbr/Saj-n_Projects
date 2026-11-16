package com.oih.idp.sp;

import com.oih.idp.identity.domain.User;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersistentNameIdService {

    private final PersistentNameIdRepository repository;

    public PersistentNameIdService(PersistentNameIdRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String persistent(User user, ServiceProvider sp) {
        return repository
                .findByUserIdAndSpId(user.getId(), sp.getId())
                .orElseGet(() -> repository.save(new PersistentNameId(
                        user.getId(), sp.getId(), UUID.randomUUID().toString())))
                .getNameId();
    }
}
