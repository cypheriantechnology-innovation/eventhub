import { test, expect } from '@playwright/test';

test.describe('Dashboard & UI Inspection', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    // Wait for the main container to load
    await expect(page.locator('app-navbar')).toBeVisible();
  });

  test('Deve carregar a página com título e branding corporativo', async ({ page }) => {
    await expect(page).toHaveTitle(/EventHub/i);
    const brandTitle = page.locator('.brand-text .title');
    await expect(brandTitle).toContainText('EventHub');
    
    const heroTitle = page.locator('.hero-title');
    await expect(heroTitle).toBeVisible();
    await expect(heroTitle).toContainText('Plataforma Corporativa de Ingestão');
  });

  test('Deve renderizar o diagrama esquemático SVG animado do pipeline', async ({ page }) => {
    const svgPipeline = page.locator('svg.pipeline-svg');
    await expect(svgPipeline).toBeVisible();

    // Check SVG nodes (IoT Sensors, Inbound Port, Core Strategy, Production & Quality branches)
    await expect(svgPipeline.locator('text:has-text("SENSORES / IoT")')).toBeVisible();
    await expect(svgPipeline.locator('text:has-text("INBOUND PORT")')).toBeVisible();
    await expect(svgPipeline.locator('text:has-text("CORE STRATEGY")')).toBeVisible();
    await expect(svgPipeline.locator('text:has-text("PRODUCTION")')).toBeVisible();
    await expect(svgPipeline.locator('text:has-text("QUALITY")')).toBeVisible();
  });

  test('Deve exibir os 4 cards executivos de KPI de telemetria', async ({ page }) => {
    const kpiSection = page.locator('.kpi-grid');
    await expect(kpiSection).toBeVisible();

    const totalCard = page.locator('.kpi-card.card-total');
    await expect(totalCard).toBeVisible();
    await expect(totalCard.locator('.kpi-label')).toContainText('TOTAL DE EVENTOS INGERIDOS');

    const prdCard = page.locator('.kpi-card.card-production');
    await expect(prdCard).toBeVisible();
    await expect(prdCard.locator('.kpi-label')).toContainText('EVENTOS DE PRODUÇÃO');

    const qltCard = page.locator('.kpi-card.card-quality');
    await expect(qltCard).toBeVisible();
    await expect(qltCard.locator('.kpi-label')).toContainText('EVENTOS DE QUALIDADE');

    const idemCard = page.locator('.kpi-card.card-idempotency');
    await expect(idemCard).toBeVisible();
    await expect(idemCard.locator('.kpi-label')).toContainText('GARANTIA DE IDEMPOTÊNCIA');
  });
});
