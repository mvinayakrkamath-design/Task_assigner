package com.ctags.service;

import com.ctags.model.AuditLog;
import com.ctags.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(Long userId, String action, String entity, Long entityId, String details) {
        repository.save(new AuditLog(userId, action, entity, entityId, details));
    }
}
