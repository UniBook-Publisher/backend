package com.unibook.publisher.common.audit.service;

import com.unibook.publisher.common.audit.entity.AuditLogEntity;

public interface AuditLogService {

    void save(AuditLogEntity entry);
}
