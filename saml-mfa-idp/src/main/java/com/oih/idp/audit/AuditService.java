package com.oih.idp.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String eventType,
            String outcome,
            UUID actorUserId,
            HttpServletRequest request,
            String failureCategory) {
        InetAddress ip = clientIp(request);
        String correlationId = request == null ? null : request.getHeader("X-Correlation-Id");
        AuditEvent event = AuditEvent.of(eventType, outcome, actorUserId, ip, correlationId, failureCategory);
        if (request != null) {
            event.setUserAgent(trim(request.getHeader("User-Agent"), 512));
        }
        repository.save(event);
        log.info(
                "audit eventType={} outcome={} actor={} failureCategory={}",
                eventType,
                outcome,
                actorUserId,
                failureCategory);
    }

    private static InetAddress clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        try {
            return InetAddress.getByName(request.getRemoteAddr());
        } catch (Exception ex) {
            return null;
        }
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
