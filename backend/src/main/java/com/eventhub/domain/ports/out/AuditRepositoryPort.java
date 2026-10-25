package com.eventhub.domain.ports.out;

import com.eventhub.domain.model.AuditLog;
import com.eventhub.domain.model.PageResult;

import java.util.List;

public interface AuditRepositoryPort {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByEventId(String eventId);

    PageResult<AuditLog> findAll(int page, int size);
}
