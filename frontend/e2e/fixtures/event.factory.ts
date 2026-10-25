/**
 * EventHub E2E & Integration Test Fixture Factory
 * Padrão AAA & Clean Test Data Engineering
 */

export interface EventFixtureOptions {
  prefix?: string;
  type?: 'PRODUCTION' | 'QUALITY';
  description?: string;
  timestamp?: string;
}

export interface GeneratedEventPayload {
  eventId: string;
  eventType: 'PRODUCTION' | 'QUALITY';
  description: string;
  timestamp?: string;
}

/**
 * Creates a valid, unique event payload with deterministic defaults
 */
export function buildEventFixture(options: EventFixtureOptions = {}): GeneratedEventPayload {
  const uniqueId = Date.now() + Math.floor(Math.random() * 1000);
  const type = options.type || 'PRODUCTION';
  const prefix = options.prefix || (type === 'QUALITY' ? 'EVT-QLT' : 'EVT-PRD');

  return {
    eventId: `${prefix}-${uniqueId}`,
    eventType: type,
    description: options.description || `Telemetria industrial automatizada gerada em ${new Date().toISOString()}`,
    timestamp: options.timestamp || new Date().toISOString()
  };
}

/**
 * Generates valid RFC 4180 CSV content with embedded quotes, commas and multiple records
 */
export function buildCsvBatchContent(records: GeneratedEventPayload[]): string {
  const header = 'eventId,eventType,timestamp,description\n';
  const rows = records.map(r => {
    // Escapes inner quotes per RFC 4180
    const escapedDesc = `"${r.description.replace(/"/g, '""')}"`;
    return `${r.eventId},${r.eventType},${r.timestamp || new Date().toISOString()},${escapedDesc}`;
  }).join('\n');

  return header + rows + '\n';
}
