import { describe, it, expect, vi, beforeEach } from 'vitest';
import { of, throwError } from 'rxjs';
import { EventService } from './event.service';
import { CreateEventRequest, IndustrialEvent, EventStats, BatchUploadResponse } from '../models/event.model';

describe('EventService (Unit Tests - Vitest)', () => {
  let service: EventService;
  let httpClientMock: any;
  let ngZoneMock: any;

  beforeEach(() => {
    httpClientMock = {
      get: vi.fn(),
      post: vi.fn()
    };
    ngZoneMock = {
      run: vi.fn((fn: () => void) => fn())
    };
    service = new EventService(httpClientMock as any, ngZoneMock as any);
  });

  describe('createEvent()', () => {
    it('deve despachar POST /api/events com o payload correto e retornar evento criado (Padrão AAA)', async () => {
      // Arrange
      const requestPayload: CreateEventRequest = {
        eventId: 'EVT-TEST-1001',
        eventType: 'PRODUCTION',
        description: 'Vibração anormal no motor 04',
        timestamp: '2026-10-25T13:00:00Z'
      };

      const mockResponse: IndustrialEvent = {
        id: 'uuid-test-1',
        eventId: 'EVT-TEST-1001',
        eventType: 'PRODUCTION',
        description: 'Vibração anormal no motor 04',
        timestamp: '2026-10-25T13:00:00Z',
        createdAt: '2026-10-25T13:00:01Z'
      };

      httpClientMock.post.mockReturnValue(of(mockResponse));

      // Act
      const result$ = service.createEvent(requestPayload);

      // Assert
      result$.subscribe(res => {
        expect(res).toEqual(mockResponse);
        expect(res.eventId).toBe('EVT-TEST-1001');
      });

      expect(httpClientMock.post).toHaveBeenCalledTimes(1);
      expect(httpClientMock.post).toHaveBeenCalledWith('/api/events', requestPayload);
    });

    it('deve propagar erro quando a API retornar 409 Conflito de Idempotência', async () => {
      // Arrange
      const duplicatePayload: CreateEventRequest = {
        eventId: 'EVT-DUPLICATE',
        eventType: 'QUALITY',
        description: 'Evento duplicado'
      };

      const mockError = { status: 409, message: 'Event ID already exists' };
      httpClientMock.post.mockReturnValue(throwError(() => mockError));

      // Act & Assert
      service.createEvent(duplicatePayload).subscribe({
        next: () => {
          throw new Error('Não deveria ter tido sucesso');
        },
        error: (err) => {
          expect(err.status).toBe(409);
          expect(err.message).toBe('Event ID already exists');
        }
      });
    });
  });

  describe('getEventStats()', () => {
    it('deve consultar GET /api/events/stats e retornar os contadores precisos', () => {
      // Arrange
      const mockStats: EventStats = {
        total: 181,
        production: 93,
        quality: 88
      };
      httpClientMock.get.mockReturnValue(of(mockStats));

      // Act
      service.getEventStats().subscribe(stats => {
        expect(stats.total).toBe(181);
        expect(stats.production).toBe(93);
        expect(stats.quality).toBe(88);
      });

      // Assert
      expect(httpClientMock.get).toHaveBeenCalledWith('/api/events/stats');
    });
  });

  describe('getEvents()', () => {
    it('deve configurar HttpParams de paginação e ordenação padrão', () => {
      // Arrange
      httpClientMock.get.mockReturnValue(of({ content: [], totalElements: 0 }));

      // Act
      service.getEvents(0, 10, 'desc').subscribe();

      // Assert
      expect(httpClientMock.get).toHaveBeenCalledTimes(1);
      const [url, options] = httpClientMock.get.mock.calls[0];
      expect(url).toBe('/api/events');
      expect(options.params.get('page')).toBe('0');
      expect(options.params.get('size')).toBe('10');
      expect(options.params.get('sort')).toBe('desc');
    });
  });

  describe('notifyEventCreated()', () => {
    it('deve emitir o evento criado através do Subject eventCreated$', () => {
      // Arrange
      const mockEvent: IndustrialEvent = {
        id: 'uuid-notify',
        eventId: 'EVT-NOTIFY-99',
        eventType: 'QUALITY',
        description: 'Auditoria emitida',
        timestamp: '2026-10-25T13:00:00Z',
        createdAt: '2026-10-25T13:00:01Z'
      };

      let receivedEvent: IndustrialEvent | null = null;
      service.eventCreated$.subscribe(ev => {
        receivedEvent = ev;
      });

      // Act
      service.notifyEventCreated(mockEvent);

      // Assert
      expect(receivedEvent).not.toBeNull();
      expect(receivedEvent!.eventId).toBe('EVT-NOTIFY-99');
      expect(receivedEvent!.eventType).toBe('QUALITY');
    });
  });
});
