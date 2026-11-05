package com.nexahire.service;

import com.nexahire.entity.AuditLog;
import com.nexahire.entity.User;
import com.nexahire.repository.AuditLogRepository;
import com.nexahire.repository.UserRepository;
import com.nexahire.security.SecurityUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public void log(String action, String entityType, Long entityId, String details, String ipAddress) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            User user = null;
            if (username != null) {
                user = userRepository.findByUsername(username).or(() -> userRepository.findByEmail(username)).orElse(null);
            }

            AuditLog log = new AuditLog(user, action, entityType, entityId, details, ipAddress);
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Never fail parent business transaction because of audit log failure
            System.err.println("Audit log error: " + e.getMessage());
        }
    }

    public void log(String action, String entityType, Long entityId, String details) {
        log(action, entityType, entityId, details, "127.0.0.1");
    }
}
