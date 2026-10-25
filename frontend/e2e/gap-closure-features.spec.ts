import { test, expect } from '@playwright/test';
import * as path from 'path';
import * as fs from 'fs';

test.describe('End-to-End Gap Closure UI Flows', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.form-card')).toBeVisible();
    await expect(page.locator('.table-card')).toBeVisible();
  });

  test('Deve inspecionar trilha imutável de auditoria no modal para evento de QUALIDADE', async ({ page }) => {
    // 1. Ingest a QUALITY event to ensure an audit log exists
    const qltEventId = `EVT-QLT-AUDIT-${Date.now()}`;
    await page.fill('input[formControlName="eventId"]', qltEventId);
    await page.click('mat-select[formControlName="eventType"]');
    await page.click('mat-option[value="QUALITY"]');
    await page.fill('textarea[formControlName="description"]', 'Inspecao de Trilha de Auditoria Imutavel');
    await page.click('button.submit-action-btn');

    // Wait for row in table
    const row = page.locator('table.event-table tr', { hasText: qltEventId });
    await expect(row).toBeVisible({ timeout: 10000 });

    // 2. Click the Auditoria button in that row
    const auditBtn = row.locator('.btn-inspect-audit');
    await expect(auditBtn).toBeVisible();
    await auditBtn.click();

    // 3. Verify the Audit Dialog opens
    const dialog = page.locator('app-audit-dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog.locator('.dialog-title')).toContainText('Trilha de Auditoria Imutável');
    await expect(dialog.locator('.dialog-subtitle code').first()).toContainText(qltEventId);

    // 4. Verify audit entries table rendered with action and timestamp
    const auditTable = dialog.locator('table.audit-table');
    await expect(auditTable).toBeVisible();
    await expect(auditTable.locator('.action-pill')).toContainText('QUALITY_EVENT_REGISTERED');

    // 5. Close dialog
    await dialog.locator('.close-btn').click();
    await expect(dialog).not.toBeVisible();
  });

  test('Deve processar ingestão em lote via upload de arquivo CSV com feedback e atualização', async ({ page }) => {
    const batchId1 = `EVT-CSV-${Date.now()}-A`;
    const batchId2 = `EVT-CSV-${Date.now()}-B`;
    const csvContent = `eventId,eventType,timestamp,description\n${batchId1},PRODUCTION,2026-10-25T03:00:00Z,Sensor de Pressao Bloco A\n${batchId2},QUALITY,2026-10-25T03:00:01Z,Analise Quimica Batelada B\n`;

    // Create temp file for upload
    const tempFilePath = path.join(process.cwd(), `temp-batch-${Date.now()}.csv`);
    fs.writeFileSync(tempFilePath, csvContent, 'utf8');

    try {
      // Find file input and upload
      const fileInput = page.locator('input#batchFileInput');
      await fileInput.setInputFiles(tempFilePath);

      // Verify SnackBar success toast with batch summary
      const snackBar = page.locator('mat-snack-bar-container.snack-success');
      await expect(snackBar).toBeVisible({ timeout: 10000 });
      await expect(snackBar).toContainText('ingeridos');

      // Verify both events appear in the telemetry table
      await expect(page.locator(`text=${batchId1}`)).toBeVisible({ timeout: 10000 });
      await expect(page.locator(`text=${batchId2}`)).toBeVisible({ timeout: 10000 });
    } finally {
      if (fs.existsSync(tempFilePath)) {
        fs.unlinkSync(tempFilePath);
      }
    }
  });

  test('Deve conectar e receber dados do Server-Sent Events (SSE) em tempo real', async ({ page }) => {
    // Check for the Live Streaming indicator badge on the table
    const liveBadge = page.locator('.live-stream-badge');
    await expect(liveBadge).toBeVisible();
    await expect(liveBadge).toContainText('SSE REAL-TIME');

    // Submit an event and confirm it reflects in the live UI
    const sseEventId = `EVT-SSE-${Date.now()}`;
    await page.fill('input[formControlName="eventId"]', sseEventId);
    await page.click('mat-select[formControlName="eventType"]');
    await page.click('mat-option[value="PRODUCTION"]');
    await page.fill('textarea[formControlName="description"]', 'Teste de Propagacao SSE');
    await page.click('button.submit-action-btn');

    // Confirm presence in table
    await expect(page.locator('table.event-table').locator(`text=${sseEventId}`)).toBeVisible({ timeout: 10000 });
  });

  test('Deve exibir menu mobile e gaveta de navegação em viewport responsivo pequeno', async ({ page }) => {
    // Simulate iPhone 12/13 viewport (390 x 844)
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto('/', { waitUntil: 'domcontentloaded' });

    const mobileMenuBtn = page.locator('button.mobile-menu-btn');
    await expect(mobileMenuBtn).toBeVisible();

    // Open drawer
    await mobileMenuBtn.click();
    const mobileDrawer = page.locator('.mobile-drawer');
    await expect(mobileDrawer).toBeVisible();
    await expect(mobileDrawer.locator('text=Pipelines Overview')).toBeVisible();
    await expect(mobileDrawer.locator('text=Event Ingestion')).toBeVisible();

    // Click link and verify drawer closes
    await mobileDrawer.locator('text=Event Ingestion').click();
    await expect(mobileDrawer).not.toBeVisible();
  });

  test('Deve processar CSV com descrições contendo vírgulas delimitadas por aspas segundo RFC 4180', async ({ page }) => {
    const csvQuotedId = `EVT-QUOTED-${Date.now()}`;
    const csvContent = `eventId,eventType,timestamp,description\n${csvQuotedId},QUALITY,2026-10-25T03:00:00Z,"Sensor óptico, calibrado no setor B"\n`;

    const tempFilePath = path.join(process.cwd(), `temp-quoted-${Date.now()}.csv`);
    fs.writeFileSync(tempFilePath, csvContent, 'utf8');

    try {
      const fileInput = page.locator('input#batchFileInput');
      await fileInput.setInputFiles(tempFilePath);

      const snackBar = page.locator('mat-snack-bar-container.snack-success');
      await expect(snackBar).toBeVisible({ timeout: 10000 });
      await expect(page.locator(`text=${csvQuotedId}`)).toBeVisible({ timeout: 10000 });
    } finally {
      if (fs.existsSync(tempFilePath)) {
        fs.unlinkSync(tempFilePath);
      }
    }
  });
});

test.describe('Observability, Tracing & Prometheus API', () => {
  test('Deve rastrear requisições com cabeçalho de correlação X-Correlation-Id', async ({ request }) => {
    const response = await request.get('http://localhost:8082/api/events');
    expect(response.status()).toBe(200);
    const headers = response.headers();
    expect(headers['x-correlation-id']).toBeDefined();
    expect(headers['x-correlation-id']).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i);
  });

  test('Deve expor métricas operacionais Prometheus no endpoint autenticado do Actuator', async ({ request }) => {
    const authHeader = 'Basic ' + Buffer.from('admin:admin').toString('base64');
    const response = await request.get('http://localhost:8082/actuator/prometheus', {
      headers: {
        Authorization: authHeader
      }
    });
    expect(response.status()).toBe(200);
    const body = await response.text();
    expect(body).toContain('events_quality_total');
    expect(body).toContain('jvm_classes_loaded_classes');
    expect(body).toContain('hikaricp_connections_min');
  });
});
