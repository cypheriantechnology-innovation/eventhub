package com.eventhub.application.usecase;

import com.eventhub.domain.model.AuditLog;
import com.eventhub.domain.model.PageResult;
import com.eventhub.domain.ports.in.FindAuditLogsUseCase;
import com.eventhub.domain.ports.out.AuditRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FindAuditLogsService implements FindAuditLogsUseCase {

    private final AuditRepositoryPort auditRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findByEventId(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return auditRepositoryPort.findByEventId(eventId.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AuditLog> findAll(int page, int size) {
        return auditRepositoryPort.findAll(page, size);
    }
}
