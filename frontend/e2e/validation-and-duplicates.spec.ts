import { test, expect } from '@playwright/test';

test.describe('Form Validation & Duplicate Idempotency Rejection', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.form-card')).toBeVisible();
  });

  test('Deve exibir erros de validação quando campos obrigatórios forem tocados e deixados vazios', async ({ page }) => {
    const eventIdInput = page.locator('input[formControlName="eventId"]');
    await eventIdInput.focus();
    await eventIdInput.blur();

    const eventIdError = page.locator('mat-error:has-text("identificador do evento é obrigatório")');
    await expect(eventIdError).toBeVisible();

    const descInput = page.locator('textarea[formControlName="description"]');
    await descInput.focus();
    await descInput.blur();

    const descError = page.locator('mat-error:has-text("descrição da ocorrência é obrigatória")');
    await expect(descError).toBeVisible();
  });

  test('Deve rejeitar evento duplicado com erro de conflito 409 e alerta visual', async ({ page }) => {
    const duplicateId = `EVT-IDEMP-E2E-${Date.now()}`;

    // 1st ingestion: should succeed
    await page.fill('input[formControlName="eventId"]', duplicateId);
    await page.click('mat-select[formControlName="eventType"]');
    await page.click('mat-option[value="PRODUCTION"]');
    await page.fill('textarea[formControlName="description"]', 'Primeiro envio legítimo');
    await page.click('button.submit-action-btn');

    // Wait for first success toast
    const firstToast = page.locator('mat-snack-bar-container');
    await expect(firstToast).toBeVisible({ timeout: 10000 });
    await expect(firstToast).toContainText('sucesso');

    // Wait for toast to disappear or click elsewhere
    await page.waitForTimeout(2500);

    // 2nd ingestion with EXACT same eventId: should be rejected
    await page.fill('input[formControlName="eventId"]', duplicateId);
    await page.click('mat-select[formControlName="eventType"]');
    await page.click('mat-option[value="PRODUCTION"]');
    await page.fill('textarea[formControlName="description"]', 'Tentativa de duplicata maliciosa');
    await page.click('button.submit-action-btn');

    // SnackBar should alert of duplicate error
    const secondToast = page.locator('mat-snack-bar-container.snack-error, mat-snack-bar-container').last();
    await expect(secondToast).toBeVisible({ timeout: 10000 });
    await expect(secondToast).toContainText(/já foi processado|duplicad|409|Erro/i);
  });
});
