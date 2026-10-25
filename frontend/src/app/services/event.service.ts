import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { CreateEventRequest, IndustrialEvent, PageResponse } from '../models/event.model';

@Injectable({
  providedIn: 'root'
})
export class EventService {

  private readonly apiUrl = '/api/events';
  private eventCreatedSource = new Subject<IndustrialEvent>();

  public eventCreated$ = this.eventCreatedSource.asObservable();

  constructor(private http: HttpClient) {}

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

  getEventStats(): Observable<import('../models/event.model').EventStats> {
    return this.http.get<import('../models/event.model').EventStats>(`${this.apiUrl}/stats`);
  }
}
