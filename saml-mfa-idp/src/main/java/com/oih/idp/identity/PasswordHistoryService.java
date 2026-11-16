package com.oih.idp.identity;

import com.oih.idp.identity.domain.PasswordHistoryEntry;
import com.oih.idp.identity.domain.User;
import com.oih.idp.identity.repo.PasswordHistoryRepository;
import com.oih.idp.policy.Argon2PasswordHasher;
import com.oih.idp.policy.PasswordPolicy;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordHistoryService {

    private final PasswordHistoryRepository repository;
    private final Argon2PasswordHasher hasher;
    private final PasswordPolicy policy;

    public PasswordHistoryService(
            PasswordHistoryRepository repository,
            Argon2PasswordHasher hasher,
            PasswordPolicy policy) {
        this.repository = repository;
        this.hasher = hasher;
        this.policy = policy;
    }

    @Transactional
    public void record(User user, String passwordHash) {
        repository.save(new PasswordHistoryEntry(user, passwordHash));
        List<PasswordHistoryEntry> history = repository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (history.size() > policy.historySize()) {
            repository.deleteAll(history.subList(policy.historySize(), history.size()));
        }
    }

    public boolean reused(User user, String candidate) {
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .limit(policy.historySize())
                .anyMatch(entry -> hasher.matches(candidate, entry.getPasswordHash()));
    }
}
