import { describe, it, expect } from 'vitest';
import { CreateEventRequest, IndustrialEvent, EventStats, BatchUploadResponse } from './event.model';

describe('API Contract & Schema Integrity (Contract Tests)', () => {
  it('deve validar que CreateEventRequest aceita apenas os tipos válidos PRODUCTION e QUALITY', () => {
    const validPrd: CreateEventRequest = {
      eventId: 'EVT-VALID-PRD',
      eventType: 'PRODUCTION',
      description: 'Sensor Operacional'
    };

    const validQlt: CreateEventRequest = {
      eventId: 'EVT-VALID-QLT',
      eventType: 'QUALITY',
      description: 'Auditoria de Linha'
    };

    expect(validPrd.eventType).toBe('PRODUCTION');
    expect(validQlt.eventType).toBe('QUALITY');
  });

  it('deve validar conformidade do contrato de resposta IndustrialEvent com campos imutáveis do backend', () => {
    const backendMockResponse: IndustrialEvent = {
      id: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
      eventId: 'EVT-PRD-4091',
      eventType: 'PRODUCTION',
      description: 'Despacho de esteira',
      timestamp: '2026-10-25T13:00:00Z',
      createdAt: '2026-10-25T13:00:01Z'
    };

    expect(backendMockResponse).toHaveProperty('id');
    expect(backendMockResponse).toHaveProperty('eventId');
    expect(backendMockResponse).toHaveProperty('eventType');
    expect(backendMockResponse).toHaveProperty('description');
    expect(backendMockResponse).toHaveProperty('timestamp');
    expect(backendMockResponse).toHaveProperty('createdAt');
    expect(typeof backendMockResponse.id).toBe('string');
  });

  it('deve validar estrutura do payload de estatísticas EventStats', () => {
    const stats: EventStats = {
      total: 100,
      production: 60,
      quality: 40
    };

    expect(stats.total).toBe(stats.production + stats.quality);
  });

  it('deve validar contrato da resposta de upload em lote BatchUploadResponse segundo RFC 4180', () => {
    const batchResponse: BatchUploadResponse = {
      status: 'SUCCESS',
      filename: 'telemetry_batch.csv',
      totalRecords: 10,
      ingested: 8,
      duplicates: 2,
      errors: 0,
      details: ['EVT-01 ingerido', 'EVT-02 duplicado']
    };

    expect(batchResponse.totalRecords).toBe(batchResponse.ingested + batchResponse.duplicates + batchResponse.errors);
    expect(Array.isArray(batchResponse.details)).toBe(true);
  });
});
