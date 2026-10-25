package com.eventhub.domain.ports.out;

import com.eventhub.domain.model.AuditLog;

import java.util.List;

public interface AuditRepositoryPort {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByEventId(String eventId);
}
