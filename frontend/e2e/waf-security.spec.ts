import { test, expect } from '@playwright/test';

test.describe('WAF Security & Attack Mitigation', () => {
  const backendBaseUrl = 'http://localhost:8082';

  test('Deve bloquear tentativa de Path Traversal na API com 403 Forbidden e incidentId', async ({ request }) => {
    const response = await request.get(`${backendBaseUrl}/api/events?file=../../../../etc/passwd`);
    
    expect(response.status()).toBe(403);
    const body = await response.json();
    expect(body.status).toBe(403);
    expect(body.error).toBe('Forbidden');
    expect(body.message).toContain('Security violation detected');
    expect(body.incidentId).toBeDefined();
    expect(body.incidentId.length).toBeGreaterThan(10);
  });

  test('Deve bloquear tentativa de SQL Injection na API com 403 Forbidden', async ({ request }) => {
    const response = await request.get(`${backendBaseUrl}/api/events?filter=' UNION SELECT * FROM users --`);
    
    expect(response.status()).toBe(403);
    const body = await response.json();
    expect(body.status).toBe(403);
    expect(body.error).toBe('Forbidden');
    expect(body.incidentId).toBeDefined();
  });

  test('Deve bloquear tentativa de Cross-Site Scripting (XSS) com 403 Forbidden', async ({ request }) => {
    const response = await request.get(`${backendBaseUrl}/api/events?q=<script>alert("xss")</script>`);
    
    expect(response.status()).toBe(403);
    const body = await response.json();
    expect(body.status).toBe(403);
    expect(body.error).toBe('Forbidden');
    expect(body.incidentId).toBeDefined();
  });

  test('Deve permitir requisições legítimas com 200 OK', async ({ request }) => {
    const response = await request.get(`${backendBaseUrl}/api/events?page=0&size=10`);
    
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.content).toBeDefined();
    expect(Array.isArray(body.content)).toBe(true);
  });
});
