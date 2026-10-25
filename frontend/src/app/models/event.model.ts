export type EventType = 'PRODUCTION' | 'QUALITY';

export interface IndustrialEvent {
  id: string;
  eventId: string;
  eventType: EventType;
  description: string;
  timestamp: string;
  createdAt: string;
}

export interface CreateEventRequest {
  eventId: string;
  eventType: EventType;
  description: string;
  timestamp?: string;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp: string;
  details?: string[];
}

export interface EventStats {
  total: number;
  production: number;
  quality: number;
}

export interface AuditLog {
  id: string;
  eventId: string;
  action: string;
  createdAt: string;
}

export interface BatchUploadResponse {
  status: string;
  filename: string;
  totalRecords: number;
  ingested: number;
  duplicates: number;
  errors: number;
  details: string[];
}

