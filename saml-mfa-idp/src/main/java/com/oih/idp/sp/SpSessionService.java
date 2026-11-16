package com.oih.idp.sp;

import com.oih.idp.identity.domain.User;
import com.oih.idp.session.IdpSsoSession;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpSessionService {

    private final SpSessionRepository repository;
    private final PersistentNameIdService nameIds;

    public SpSessionService(SpSessionRepository repository, PersistentNameIdService nameIds) {
        this.repository = repository;
        this.nameIds = nameIds;
    }

    @Transactional
    public void bind(IdpSsoSession session, ServiceProvider sp, User user) {
        repository.save(new SpSession(
                session.getId(), sp.getId(), session.getSessionIndex(), nameIds.persistent(user, sp)));
    }

    public List<SpSession> forIdpSession(java.util.UUID idpSessionId) {
        return repository.findByIdpSessionId(idpSessionId);
    }

    @Transactional
    public void markLogout(SpSession session, String status) {
        session.setSloStatus(status);
        repository.save(session);
    }
}
