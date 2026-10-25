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
