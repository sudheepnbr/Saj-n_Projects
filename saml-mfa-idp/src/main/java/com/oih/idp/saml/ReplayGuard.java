package com.oih.idp.saml;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReplayGuard {

    private final JdbcTemplate jdbc;

    public ReplayGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void consume(String messageId, String type, Instant expiresAt) {
        String hash = sha256(messageId);
        try {
            jdbc.update(
                    "INSERT INTO replay_cache (id_hash, message_type, expires_at) VALUES (?,?,?)",
                    hash,
                    type,
                    java.sql.Timestamp.from(expiresAt));
        } catch (DataIntegrityViolationException ex) {
            throw new SamlProtocolException("SAML_REPLAY", "Replay detected");
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
