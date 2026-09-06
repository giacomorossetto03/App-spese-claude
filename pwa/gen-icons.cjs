const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const iconSVG = (maskable) => {
  const inner = `
    <g fill="#ffffff">
      <rect x="110" y="310" width="60" height="90" rx="14"/>
      <rect x="194" y="250" width="60" height="150" rx="14"/>
      <rect x="278" y="188" width="60" height="212" rx="14"/>
      <rect x="362" y="126" width="60" height="274" rx="14"/>
    </g>`;
  const content = maskable
    ? `<g transform="translate(256,256) scale(0.66) translate(-256,-256)">${inner}</g>`
    : inner;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
    <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#37a884"/><stop offset="1" stop-color="#1e5c48"/></linearGradient></defs>
    <rect width="512" height="512" fill="url(#g)"/>
    ${content}
  </svg>`;
};

(async () => {
  let browser;
  try { browser = await chromium.launch(); }
  catch (e) { browser = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium' }); }
  const page = await browser.newPage();
  const dir = path.join(__dirname, 'icons');
  fs.mkdirSync(dir, { recursive: true });
  const targets = [
    { file: 'icon-180.png', size: 180, maskable: false },
    { file: 'icon-192.png', size: 192, maskable: false },
    { file: 'icon-512.png', size: 512, maskable: false },
    { file: 'icon-maskable-512.png', size: 512, maskable: true }
  ];
  for (const t of targets) {
    const svg = iconSVG(t.maskable);
    const html = `<!DOCTYPE html><html><head><meta charset="utf-8"><style>*{margin:0;padding:0}html,body{width:${t.size}px;height:${t.size}px;overflow:hidden}svg{display:block;width:${t.size}px;height:${t.size}px}</style></head><body>${svg}</body></html>`;
    await page.setViewportSize({ width: t.size, height: t.size });
    await page.setContent(html, { waitUntil: 'networkidle' });
    const elt = await page.$('svg');
    await elt.screenshot({ path: path.join(dir, t.file) });
    console.log('wrote', t.file, t.size + 'px');
  }
  await browser.close();
})();
