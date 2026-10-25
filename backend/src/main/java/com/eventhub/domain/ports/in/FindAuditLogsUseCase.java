package com.eventhub.domain.ports.in;

import com.eventhub.domain.model.AuditLog;
import com.eventhub.domain.model.PageResult;

import java.util.List;

public interface FindAuditLogsUseCase {

    List<AuditLog> findByEventId(String eventId);

    PageResult<AuditLog> findAll(int page, int size);
}
