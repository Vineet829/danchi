package com.danchi.auth;

import java.time.Duration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PendingSignUps {
    static final Duration KEPT = Duration.ofDays(7);
    private static final String PENDING = "NOT u.is_active AND u.activated_at IS NULL AND u.last_login IS NULL"
            + " AND NOT u.is_guest AND NOT u.is_staff AND NOT u.is_superuser"
            + " AND NOT EXISTS (SELECT 1 FROM admin_audit_log h WHERE h.resource = 'users' AND h.object_pkid = u.pkid)";

    private final JdbcTemplate db;

    public PendingSignUps(JdbcTemplate db) {
        this.db = db;
    }

    public boolean removeFor(String email) {
        return db.update("DELETE FROM users u WHERE lower(u.email) = lower(?) AND " + PENDING, email.trim()) > 0;
    }

    public int removeExpired() {
        return db.update(
                "DELETE FROM users u WHERE u.date_joined < NOW() - make_interval(days => ?) AND " + PENDING,
                (int) KEPT.toDays());
    }
}
