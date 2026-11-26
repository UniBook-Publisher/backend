package com.unibook.publisher.common.audit.service;

import com.unibook.publisher.common.audit.entity.AuditLogEntity;
import com.unibook.publisher.common.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(AuditLogEntity entry) {
        auditLogRepository.save(entry);
    }
}
