import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditLog, PageResponse } from '../models/event.model';

@Injectable({
  providedIn: 'root'
})
export class AuditService {

  private readonly apiUrl = '/api/audit-logs';

  constructor(private http: HttpClient) {}

  getAuditLogsByEventId(eventId: string): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.apiUrl}/${encodeURIComponent(eventId)}`);
  }

  getAllAuditLogs(page: number = 0, size: number = 20): Observable<PageResponse<AuditLog>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<AuditLog>>(this.apiUrl, { params });
  }
}
