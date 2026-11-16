package com.oih.idp.session;

import com.oih.idp.config.IdpProperties;
import com.oih.idp.web.security.IdpUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SsoSessionService {

    public static final String ATTR_SESSION_INDEX = "OIH_SESSION_INDEX";

    private final IdpSsoSessionRepository sessions;
    private final IdpProperties properties;
    private final JdbcTemplate jdbc;

    public SsoSessionService(IdpSsoSessionRepository sessions, IdpProperties properties, JdbcTemplate jdbc) {
        this.sessions = sessions;
        this.properties = properties;
        this.jdbc = jdbc;
    }

    @Transactional
    public IdpSsoSession establish(IdpUserPrincipal principal, HttpServletRequest request) {
        HttpSession http = request.getSession(true);
        Instant expiry = Instant.now().plus(properties.getSession().getAbsolute());
        IdpSsoSession sso = IdpSsoSession.open(principal.getUserId(), http.getId(), expiry);
        sessions.save(sso);
        http.setAttribute(ATTR_SESSION_INDEX, sso.getSessionIndex());
        capConcurrent(principal.getUserId());
        return sso;
    }

    public IdpSsoSession current(HttpServletRequest request) {
        HttpSession http = request.getSession(false);
        if (http == null) {
            return null;
        }
        Object index = http.getAttribute(ATTR_SESSION_INDEX);
        if (index instanceof String sessionIndex) {
            return sessions.findBySessionIndex(sessionIndex).filter(IdpSsoSession::isActive).orElse(null);
        }
        return sessions.findByHttpSessionId(http.getId()).filter(IdpSsoSession::isActive).orElse(null);
    }

    @Transactional
    public void revoke(UUID idpSessionId) {
        IdpSsoSession session = sessions.findById(idpSessionId).orElseThrow();
        session.revoke();
        sessions.save(session);
        if (session.getHttpSessionId() != null) {
            jdbc.update("DELETE FROM SPRING_SESSION WHERE SESSION_ID = ?", session.getHttpSessionId());
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            SecurityContextHolder.clearContext();
        }
    }

    @Transactional
    public void revokeHttp(String httpSessionId) {
        sessions.findByHttpSessionId(httpSessionId).ifPresent(session -> {
            session.revoke();
            sessions.save(session);
        });
        jdbc.update("DELETE FROM SPRING_SESSION WHERE SESSION_ID = ?", httpSessionId);
    }

    public List<IdpSsoSession> forUser(UUID userId) {
        return sessions.findByUserIdAndRevokedAtIsNull(userId);
    }

    private void capConcurrent(UUID userId) {
        List<IdpSsoSession> live = sessions.findByUserIdAndRevokedAtIsNull(userId);
        int max = properties.getSession().getMaxConcurrent();
        if (live.size() <= max) {
            return;
        }
        live.stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .limit(live.size() - max)
                .forEach(this::revokeQuiet);
    }

    private void revokeQuiet(IdpSsoSession session) {
        session.revoke();
        sessions.save(session);
        if (session.getHttpSessionId() != null) {
            jdbc.update("DELETE FROM SPRING_SESSION WHERE SESSION_ID = ?", session.getHttpSessionId());
        }
    }
}
