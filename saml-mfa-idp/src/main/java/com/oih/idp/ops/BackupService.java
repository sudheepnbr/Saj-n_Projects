package com.oih.idp.ops;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BackupService {

    private final JdbcTemplate jdbc;

    public BackupService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> snapshotIdentity() {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("users", jdbc.queryForList("SELECT id, username, email, enabled FROM users ORDER BY username"));
        snap.put("signing_keys", jdbc.queryForList("SELECT id, status, fingerprint_sha256 FROM signing_keys ORDER BY status"));
        snap.put("service_providers", jdbc.queryForList("SELECT id, entity_id, enabled FROM service_providers ORDER BY entity_id"));
        return snap;
    }

    @Transactional
    public void restoreUsersEnabled(Map<String, Object> snapshot) {
        @SuppressWarnings("unchecked")
        var users = (java.util.List<Map<String, Object>>) snapshot.get("users");
        for (Map<String, Object> user : users) {
            jdbc.update(
                    "UPDATE users SET enabled = ? WHERE id = ?",
                    user.get("enabled"),
                    user.get("id"));
        }
    }
}
