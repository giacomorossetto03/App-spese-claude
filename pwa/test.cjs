const { chromium } = require('playwright');
const http = require('http');
const fs = require('fs');
const path = require('path');

const ROOT = __dirname;
const MIME = { '.html':'text/html','.js':'text/javascript','.css':'text/css','.json':'application/json','.webmanifest':'application/manifest+json','.png':'image/png' };

const server = http.createServer((req, res) => {
  let p = decodeURIComponent(req.url.split('?')[0]);
  if (p === '/' ) p = '/index.html';
  const file = path.join(ROOT, p);
  fs.readFile(file, (err, data) => {
    if (err) { res.writeHead(404); res.end('nf'); return; }
    res.writeHead(200, { 'Content-Type': MIME[path.extname(file)] || 'application/octet-stream' });
    res.end(data);
  });
});

(async () => {
  await new Promise(r => server.listen(0, r));
  const port = server.address().port;
  const base = `http://localhost:${port}/`;
  fs.mkdirSync(path.join(ROOT, 'shots'), { recursive: true });

  let browser;
  try { browser = await chromium.launch(); }
  catch (e) { browser = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium' }); }
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  const errors = [];
  page.on('console', m => { if (m.type() === 'error') errors.push('CONSOLE: ' + m.text()); });
  page.on('pageerror', e => errors.push('PAGEERROR: ' + e.message));

  const shot = (n) => page.screenshot({ path: path.join(ROOT, 'shots', n) });
  const wait = (ms) => page.waitForTimeout(ms);

  try {
    await page.goto(base, { waitUntil: 'load' });
    await wait(500);
    await shot('1-dashboard.png');

    // Add expense via FAB
    await page.click('#fab'); await wait(250);
    await page.fill('#f-amount', '12,50');
    await page.click('#save-expense'); await wait(250);

    // add a second expense different category
    await page.click('#fab'); await wait(250);
    await page.fill('#f-amount', '45,00');
    await page.selectOption('#f-cat', { index: 2 });
    await page.fill('#f-note', 'Spesa test');
    await page.click('#save-expense'); await wait(250);

    // Spese view
    await page.click('[data-nav="spese"]'); await wait(250);
    await shot('3-spese.png');

    // Rate: add plan with interest mode
    await page.click('[data-nav="rate"]'); await wait(150);
    await page.click('[data-act="add-plan"]'); await wait(200);
    await page.fill('#p-title', 'iPhone 15');
    await page.click('[data-mode="interest"]'); await wait(100);
    await page.fill('#p-price', '1000');
    await page.fill('#p-pct', '10');
    await page.fill('#p-count', '10');
    await wait(100);
    await page.click('#save-plan'); await wait(250);
    await shot('4-rate.png');

    // open plan detail, mark first entry paid
    await page.click('[data-open-plan]'); await wait(200);
    await page.click('[data-toggle-entry]'); await wait(200);
    await shot('5-plandetail.png');
    await page.click('.sheet button[data-scrim="1"]'); await wait(150);

    // recurring add
    await page.click('[data-nav="impostazioni"]'); await wait(120);
    await page.click('[data-act="go-ricorrenti"]'); await wait(120);
    await page.click('[data-act="add-rec"]'); await wait(200);
    await page.fill('#r-title', 'Netflix');
    await page.fill('#r-amount', '9,99');
    await page.fill('#r-day', '5');
    await page.click('#save-rec'); await wait(250);
    await shot('6-ricorrenti.png');

    // dashboard with data
    await page.click('[data-nav="dashboard"]'); await wait(300);
    await shot('7-dashboard2.png');

    // set budget then dashboard
    await page.click('[data-nav="impostazioni"]'); await wait(120);
    await page.click('[data-act="edit-budget"]'); await wait(200);
    await page.fill('#b-amount', '2000');
    await page.click('#save-budget'); await wait(200);
    // dark theme
    await page.click('[data-theme="dark"]'); await wait(200);
    await shot('8-dark-settings.png');
    await page.click('[data-nav="dashboard"]'); await wait(300);
    await shot('9-dark-dashboard.png');

    // check spese filter Rate
    await page.click('[data-nav="spese"]'); await wait(150);
    await page.click('.chip[data-filter="INSTALLMENT"]', { force: true }); await wait(250);
    const filterState = await page.evaluate(() => ({
      selChip: (document.querySelector('.chip.sel') || {}).textContent || null,
      rows: document.querySelectorAll('#app .list .row').length
    }));
    console.log('FILTER_STATE:', JSON.stringify(filterState));
    await shot('10-spese-rate-filter.png');

    // Verify persistence: reload and check state present
    await page.reload({ waitUntil: 'load' }); await wait(400);
    const persisted = await page.evaluate(() => {
      const s = JSON.parse(localStorage.getItem('spese_state_v1') || '{}');
      return { expenses: (s.expenses||[]).length, plans: (s.plans||[]).length, recurring: (s.recurring||[]).length, entries: (s.entries||[]).length };
    });
    console.log('STATE_AFTER_RELOAD:', JSON.stringify(persisted));
  } catch (e) {
    console.log('TEST_EXCEPTION:', e.message);
  }

  console.log('ERRORS_COUNT:', errors.length);
  errors.forEach(e => console.log(e));
  await browser.close();
  server.close();
})();
