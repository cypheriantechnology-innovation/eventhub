import { test, expect } from '@playwright/test';

test.describe('End-to-End Event Ingestion Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.form-card')).toBeVisible();
    await expect(page.locator('.table-card')).toBeVisible();
  });

  test('Deve ingerir evento de PRODUÇÃO manualmente com sucesso e exibir na tabela', async ({ page }) => {
    const uniqueId = `EVT-PRD-E2E-${Date.now()}`;
    const descriptionText = `Teste E2E Operacional Automatizado ${Date.now()}`;

    // Fill form
    await page.fill('input[formControlName="eventId"]', uniqueId);
    
    // Select PRODUCTION type
    await page.click('mat-select[formControlName="eventType"]');
    await page.click('mat-option[value="PRODUCTION"]');

    // Fill description
    await page.fill('textarea[formControlName="description"]', descriptionText);

    // Submit
    const submitBtn = page.locator('button.submit-action-btn');
    await expect(submitBtn).toBeEnabled();
    await submitBtn.click();

    // Verify SnackBar toast notification
    const snackBar = page.locator('mat-snack-bar-container');
    await expect(snackBar).toBeVisible({ timeout: 10000 });
    await expect(snackBar).toContainText('sucesso');

    // Verify row appears in event table
    const table = page.locator('table.event-table');
    await expect(table.locator(`text=${uniqueId}`)).toBeVisible({ timeout: 10000 });
    await expect(table.locator(`text=${descriptionText}`)).toBeVisible();
  });

  test('Deve ingerir evento de QUALIDADE com auditoria e exibir badge auditado', async ({ page }) => {
    const uniqueId = `EVT-QLT-E2E-${Date.now()}`;
    const descriptionText = `Teste E2E Auditoria Automatizado ${Date.now()}`;

    // Fill form
    await page.fill('input[formControlName="eventId"]', uniqueId);
    
    // Select QUALITY type
    await page.click('mat-select[formControlName="eventType"]');
    await page.click('mat-option[value="QUALITY"]');

    // Fill description
    await page.fill('textarea[formControlName="description"]', descriptionText);

    // Submit
    const submitBtn = page.locator('button.submit-action-btn');
    await expect(submitBtn).toBeEnabled();
    await submitBtn.click();

    // Verify SnackBar toast notification
    const snackBar = page.locator('mat-snack-bar-container');
    await expect(snackBar).toBeVisible({ timeout: 10000 });
    await expect(snackBar).toContainText('sucesso');

    // Verify row appears in event table with QUALITY badge and Auditado tag
    const row = page.locator('table.event-table tr', { hasText: uniqueId });
    await expect(row).toBeVisible({ timeout: 10000 });
    await expect(row.locator('.badge-quality')).toBeVisible();
    await expect(row.locator('.audit-active')).toContainText('Auditado (audit_logs)');
  });

  test('Deve utilizar os botões de Exemplos Rápidos para preenchimento ágil', async ({ page }) => {
    const samplePrdBtn = page.locator('button.btn-sample-prd');
    await samplePrdBtn.click();

    const eventIdInput = page.locator('input[formControlName="eventId"]');
    const value = await eventIdInput.inputValue();
    expect(value).toMatch(/^EVT-PRD-/);

    const descInput = page.locator('textarea[formControlName="description"]');
    const descValue = await descInput.inputValue();
    expect(descValue.length).toBeGreaterThan(0);
  });
});
