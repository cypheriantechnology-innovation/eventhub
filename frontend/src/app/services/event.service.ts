import { Injectable, NgZone } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { BatchUploadResponse, CreateEventRequest, EventStats, IndustrialEvent, PageResponse } from '../models/event.model';

@Injectable({
  providedIn: 'root'
})
export class EventService {

  private readonly apiUrl = '/api/events';
  private eventCreatedSource = new Subject<IndustrialEvent>();

  public eventCreated$ = this.eventCreatedSource.asObservable();

  constructor(
    private http: HttpClient,
    private zone: NgZone
  ) {}

  createEvent(request: CreateEventRequest): Observable<IndustrialEvent> {
    return this.http.post<IndustrialEvent>(this.apiUrl, request);
  }

  notifyEventCreated(event: IndustrialEvent): void {
    this.eventCreatedSource.next(event);
  }

  getEvents(page: number = 0, size: number = 10, sort: string = 'desc'): Observable<PageResponse<IndustrialEvent>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.get<PageResponse<IndustrialEvent>>(this.apiUrl, { params });
  }

  getEventById(eventId: string): Observable<IndustrialEvent> {
    return this.http.get<IndustrialEvent>(`${this.apiUrl}/${eventId}`);
  }

  getEventStats(): Observable<EventStats> {
    return this.http.get<EventStats>(`${this.apiUrl}/stats`);
  }

  uploadBatch(file: File): Observable<BatchUploadResponse> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<BatchUploadResponse>(`${this.apiUrl}/upload`, formData);
  }

  subscribeToTelemetryStream(): Observable<IndustrialEvent> {
    return new Observable<IndustrialEvent>(observer => {
      const eventSource = new EventSource(`${this.apiUrl}/stream`);

      eventSource.addEventListener('EVENT_INGESTED', (event: MessageEvent) => {
        this.zone.run(() => {
          try {
            const data: IndustrialEvent = JSON.parse(event.data);
            observer.next(data);
          } catch (err) {
            console.error('Error parsing SSE event data', err);
          }
        });
      });

      eventSource.onerror = (error) => {
        this.zone.run(() => {
          console.warn('SSE connection closed or error, will auto-reconnect', error);
        });
      };

      return () => {
        eventSource.close();
      };
    });
  }
}
