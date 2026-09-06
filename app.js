'use strict';
/* =========================================================================
   Spese — PWA offline (vanilla JS). Denaro sempre in centesimi (intero).
   Dati su localStorage (una sola sorgente JSON). Nessun backend.
   ========================================================================= */

// ---- Palette categorie (stabile per id) ---------------------------------
const PALETTE = ['#2E7D64','#4E79A7','#E1575A','#F28E2B','#76B041','#9C6ADE','#57A6A1','#D4A017','#B07AA1','#8C7B6B'];
const colorForId = (id) => PALETTE[(((id % PALETTE.length) + PALETTE.length) % PALETTE.length)];
const DEFAULT_CATEGORIES = ['Alimentari','Casa','Trasporti','Bollette','Salute','Svago','Ristoranti','Abbonamenti','Shopping','Altro'];

// ---- Utility ------------------------------------------------------------
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
const esc = (s) => String(s == null ? '' : s).replace(/[&<>"']/g, m => ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;' }[m]));

const Money = {
  fmt: new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' }),
  format(cents) { return Money.fmt.format((cents || 0) / 100); },
  parse(str) {
    if (str == null) return null;
    let s = String(str).trim().replace(/\s/g, '').replace(/€/g, '');
    if (s === '') return null;
    if (s.includes(',')) s = s.replace(/\./g, '').replace(',', '.');
    const v = parseFloat(s);
    if (isNaN(v)) return null;
    return Math.round(v * 100);
  }
};

const pad = (n) => String(n).padStart(2, '0');
function ymd(d) { return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`; }
function todayISO() { return ymd(new Date()); }
function curPeriod() { return todayISO().slice(0, 7); }
function periodOf(iso) { return iso.slice(0, 7); }
function addMonthsYm(ym, n) {
  let [y, m] = ym.split('-').map(Number);
  const tot = y * 12 + (m - 1) + n;
  return `${Math.floor(tot / 12)}-${pad((tot % 12) + 1)}`;
}
function daysInMonth(ym) { const [y, m] = ym.split('-').map(Number); return new Date(y, m, 0).getDate(); }
function addMonthsDate(iso, n) {
  const [y, m, d] = iso.split('-').map(Number);
  const base = new Date(y, m - 1 + n, 1);
  const dim = new Date(base.getFullYear(), base.getMonth() + 1, 0).getDate();
  return `${base.getFullYear()}-${pad(base.getMonth() + 1)}-${pad(Math.min(d, dim))}`;
}
const MONTHS_IT = ['Gennaio','Febbraio','Marzo','Aprile','Maggio','Giugno','Luglio','Agosto','Settembre','Ottobre','Novembre','Dicembre'];
const MONTHS_IT_SHORT = ['Gen','Feb','Mar','Apr','Mag','Giu','Lug','Ago','Set','Ott','Nov','Dic'];
function monthLabel(ym) { const [y, m] = ym.split('-').map(Number); return `${MONTHS_IT[m - 1]} ${y}`; }
function shortDate(iso) { const [, m, d] = iso.split('-').map(Number); return `${pad(d)}/${pad(m)}`; }

// ---- Store --------------------------------------------------------------
const STORAGE_KEY = 'spese_state_v1';
let state = null;

function blankState() {
  return { seq: 0, categories: [], expenses: [], plans: [], entries: [], recurring: [], settings: { theme: 'system', budgetCents: 0 } };
}
function nextId() { state.seq += 1; return state.seq; }
function save() { try { localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); } catch (e) { console.error('save failed', e); } }
function load() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) { state = JSON.parse(raw); }
  } catch (e) { console.error('load failed', e); }
  if (!state) state = blankState();
  // difese su campi mancanti
  state.settings = Object.assign({ theme: 'system', budgetCents: 0 }, state.settings || {});
  ['categories','expenses','plans','entries','recurring'].forEach(k => { if (!Array.isArray(state[k])) state[k] = []; });
  if (typeof state.seq !== 'number') state.seq = 0;
}
function seedIfEmpty() {
  if (state.categories.length === 0) {
    DEFAULT_CATEGORIES.forEach((name, i) => state.categories.push({ id: nextId(), name, archived: false, sortOrder: i }));
    save();
  }
}

// ---- Categorie ----------------------------------------------------------
const catById = (id) => state.categories.find(c => c.id === id);
const catName = (id) => { const c = catById(id); return c ? c.name : '—'; };
const activeCats = () => state.categories.filter(c => !c.archived).sort((a, b) => (a.sortOrder - b.sortOrder) || a.name.localeCompare(b.name));
function categoryInUse(id) {
  return state.expenses.some(e => e.categoryId === id) ||
    state.plans.some(p => p.categoryId === id) ||
    state.recurring.some(r => r.categoryId === id);
}

// ---- Ricorrenti: materializzazione (idempotente, con previsioni) --------
function generateRecurring(monthsAhead = 12) {
  const horizon = addMonthsYm(curPeriod(), monthsAhead);
  let changed = false;
  for (const r of state.recurring) {
    if (!r.active) continue;
    if (r.lastGeneratedPeriod && r.lastGeneratedPeriod >= horizon) continue;
    const startYm = periodOf(r.startDate);
    const endYm = r.endDate ? periodOf(r.endDate) : null;
    let period = startYm;
    if (r.lastGeneratedPeriod) {
      const nextAfter = addMonthsYm(r.lastGeneratedPeriod, 1);
      if (nextAfter > period) period = nextAfter;
    }
    let lastProcessed = r.lastGeneratedPeriod || null;
    while (period <= horizon) {
      const within = !endYm || period <= endYm;
      if (within) {
        const exists = state.expenses.some(e => e.type === 'RECURRING_INSTANCE' && e.recurringId === r.id && periodOf(e.date) === period);
        if (!exists) {
          const day = Math.min(r.dayOfMonth, daysInMonth(period));
          state.expenses.push({
            id: nextId(), amountCents: r.amountCents, date: `${period}-${pad(day)}`,
            categoryId: r.categoryId, note: r.title, type: 'RECURRING_INSTANCE', recurringId: r.id
          });
          changed = true;
        }
      }
      lastProcessed = period;
      period = addMonthsYm(period, 1);
    }
    if (lastProcessed && lastProcessed !== r.lastGeneratedPeriod) { r.lastGeneratedPeriod = lastProcessed; changed = true; }
  }
  if (changed) save();
}
function removeFutureInstances(ruleId, fromISO = todayISO()) {
  const before = state.expenses.length;
  state.expenses = state.expenses.filter(e => !(e.type === 'RECURRING_INSTANCE' && e.recurringId === ruleId && e.date >= fromISO));
  if (state.expenses.length !== before) save();
}
function syncRecurringInstances(rule) {
  state.expenses.forEach(e => {
    if (e.type === 'RECURRING_INSTANCE' && e.recurringId === rule.id) {
      e.amountCents = rule.amountCents; e.categoryId = rule.categoryId; e.note = rule.title;
    }
  });
}

// ---- Rate ---------------------------------------------------------------
function createPlan({ title, totalCents, count, firstDue, categoryId, note }) {
  const base = Math.floor(totalCents / count);
  const remainder = totalCents - base * count;
  const planId = nextId();
  state.plans.push({ id: planId, title, totalCents, count, categoryId, note: note || null, firstDue, createdAt: Date.now() });
  for (let i = 0; i < count; i++) {
    state.entries.push({
      id: nextId(), planId, number: i + 1, dueDate: addMonthsDate(firstDue, i),
      amountCents: i === count - 1 ? base + remainder : base, paid: false, paidDate: null
    });
  }
  save();
}
function deletePlan(id) {
  state.plans = state.plans.filter(p => p.id !== id);
  state.entries = state.entries.filter(e => e.planId !== id);
  save();
}
const planById = (id) => state.plans.find(p => p.id === id);
function planProgress(id) {
  const es = state.entries.filter(e => e.planId === id);
  const paid = es.filter(e => e.paid).length;
  const paidCents = es.filter(e => e.paid).reduce((s, e) => s + e.amountCents, 0);
  const total = es.reduce((s, e) => s + e.amountCents, 0);
  return { paid, count: es.length, paidCents, total };
}

// ---- Aggregati mese -----------------------------------------------------
function expensesInMonth(ym) { return state.expenses.filter(e => periodOf(e.date) === ym); }
function entriesDueInMonth(ym) { return state.entries.filter(e => periodOf(e.dueDate) === ym); }
function totalInMonth(ym) {
  return expensesInMonth(ym).reduce((s, e) => s + e.amountCents, 0) +
    entriesDueInMonth(ym).reduce((s, e) => s + e.amountCents, 0);
}
function countInMonth(ym) { return expensesInMonth(ym).length + entriesDueInMonth(ym).length; }
function categoryBreakdown(ym) {
  const by = {};
  expensesInMonth(ym).forEach(e => { by[e.categoryId] = (by[e.categoryId] || 0) + e.amountCents; });
  entriesDueInMonth(ym).forEach(e => { const p = planById(e.planId); if (p) by[p.categoryId] = (by[p.categoryId] || 0) + e.amountCents; });
  return Object.entries(by).map(([id, total]) => ({ categoryId: Number(id), total }))
    .sort((a, b) => b.total - a.total);
}
function openInstallments() {
  const open = state.entries.filter(e => !e.paid);
  return { count: open.length, residual: open.reduce((s, e) => s + e.amountCents, 0) };
}
function recentExpenses(limit = 5) {
  const t = todayISO();
  return state.expenses.filter(e => e.date <= t).sort((a, b) => (b.date < a.date ? -1 : b.date > a.date ? 1 : b.id - a.id)).slice(0, limit);
}
function monthlyTrend(ym, months = 6) {
  const first = addMonthsYm(ym, -(months - 1));
  const out = [];
  for (let i = 0; i < months; i++) {
    const m = addMonthsYm(first, i);
    out.push({ ym: m, label: MONTHS_IT_SHORT[Number(m.split('-')[1]) - 1], total: totalInMonth(m) });
  }
  return out;
}

// =========================================================================
//  RENDER
// =========================================================================
let view = 'dashboard';
let dashMonth = curPeriod();
let speseMonth = curPeriod();
let speseFilter = 'ALL';      // ALL | SINGLE | RECURRING | INSTALLMENT
let speseCatFilter = null;

function setTheme(mode) {
  const root = document.documentElement;
  if (mode === 'light' || mode === 'dark') root.setAttribute('data-theme', mode);
  else root.removeAttribute('data-theme');
}

function render() {
  const app = $('#app');
  if (view === 'dashboard') app.innerHTML = viewDashboard();
  else if (view === 'spese') app.innerHTML = viewSpese();
  else if (view === 'rate') app.innerHTML = viewRate();
  else if (view === 'impostazioni') app.innerHTML = viewImpostazioni();
  else if (view === 'categorie') app.innerHTML = viewCategorie();
  else if (view === 'ricorrenti') app.innerHTML = viewRicorrenti();
  renderNav();
  const fab = $('#fab');
  fab.hidden = !(view === 'dashboard' || view === 'spese');
  app.scrollTop = 0; window.scrollTo(0, 0);
}

function renderNav() {
  const items = [
    ['dashboard', '📊', 'Dashboard'],
    ['spese', '📃', 'Spese'],
    ['rate', '🗓️', 'Rate'],
    ['impostazioni', '⚙️', 'Impostazioni']
  ];
  const sel = (view === 'categorie' || view === 'ricorrenti') ? 'impostazioni' : view;
  $('#bottomnav').innerHTML = items.map(([id, ic, label]) =>
    `<button class="navitem ${sel === id ? 'sel' : ''}" data-nav="${id}"><span class="ic">${ic}</span>${label}</button>`
  ).join('');
}

// ---- Componenti ---------------------------------------------------------
function monthBar(ym, act) {
  return `<div class="monthbar">
    <button class="iconbtn" data-act="${act}-prev" aria-label="Mese precedente">‹</button>
    <span class="label">${monthLabel(ym)}</span>
    <button class="iconbtn" data-act="${act}-next" aria-label="Mese successivo">›</button>
  </div>`;
}
function donutSVG(slices, size = 120, stroke = 20) {
  const r = (size - stroke) / 2, c = 2 * Math.PI * r, cx = size / 2;
  if (!slices.length) return `<svg width="${size}" height="${size}"></svg>`;
  let off = 0;
  const segs = slices.map(s => {
    const len = s.frac * c;
    const seg = `<circle cx="${cx}" cy="${cx}" r="${r}" fill="none" stroke="${s.color}" stroke-width="${stroke}"
      stroke-dasharray="${len.toFixed(2)} ${(c - len).toFixed(2)}" stroke-dashoffset="${(-off).toFixed(2)}"/>`;
    off += len; return seg;
  }).join('');
  return `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" style="transform:rotate(-90deg)">${segs}</svg>`;
}

// ---- Dashboard ----------------------------------------------------------
function viewDashboard() {
  const ym = dashMonth;
  const total = totalInMonth(ym);
  const count = countInMonth(ym);
  const inst = openInstallments();
  const budget = state.settings.budgetCents || 0;
  const breakdown = categoryBreakdown(ym);
  const catTotal = breakdown.reduce((s, b) => s + b.total, 0);
  const slices = breakdown.map(b => ({ color: colorForId(b.categoryId), frac: catTotal ? b.total / catTotal : 0 }));
  const trend = monthlyTrend(ym, 6);
  const maxT = Math.max(1, ...trend.map(t => t.total));
  const recent = recentExpenses(5);

  let budgetHtml = '';
  if (budget > 0) {
    const frac = Math.min(1, total / budget);
    const over = total > budget;
    budgetHtml = `<div class="card">
      <div style="display:flex;justify-content:space-between;font-size:13px;margin-bottom:6px">
        <span>Budget mese</span><span>${Money.format(total)} / ${Money.format(budget)}</span>
      </div>
      <div class="bar ${over ? 'over' : ''}"><span style="width:${(frac * 100).toFixed(0)}%"></span></div>
      ${over ? `<div class="err" style="color:var(--danger);font-size:12px;margin-top:6px">Budget superato di ${Money.format(total - budget)}</div>` : ''}
    </div>`;
  }

  const donutHtml = catTotal > 0 ? `<div class="card">
    <div class="section-label" style="padding:0 0 8px">Ripartizione categorie</div>
    <div class="chart-wrap">
      ${donutSVG(slices)}
      <div class="legend">${breakdown.slice(0, 5).map(b => `
        <div class="li"><span class="dot" style="background:${colorForId(b.categoryId)}"></span>
        <span class="nm">${esc(catName(b.categoryId))}</span><span class="vl">${Money.format(b.total)}</span></div>`).join('')}
      </div>
    </div>
  </div>` : '';

  const trendHtml = `<div class="card">
    <div class="section-label" style="padding:0 0 8px">Andamento 6 mesi</div>
    <div class="bars">${trend.map(t => `<div class="b" title="${Money.format(t.total)}">
      <i style="height:${(t.total / maxT * 100).toFixed(0)}%; ${t.ym === curPeriod() ? '' : 'opacity:.75'}"></i>
      <small>${t.label}</small></div>`).join('')}</div>
  </div>`;

  const recentHtml = recent.length ? `<div class="section-label">Ultime spese</div>
    <div class="list">${recent.map(rowExpenseHtml).join('')}</div>` : '';

  return `
  <div class="appbar"><h1>Dashboard</h1></div>
  ${monthBar(ym, 'dash')}
  <div class="grid2">
    <div class="stat"><div class="k">Totale mese</div><div class="v">${Money.format(total)}</div></div>
    <div class="stat"><div class="k">Movimenti</div><div class="v">${count}</div></div>
    <div class="stat"><div class="k">Rate aperte</div><div class="v">${inst.count}</div></div>
    <div class="stat"><div class="k">Residuo rate</div><div class="v">${Money.format(inst.residual)}</div></div>
  </div>
  ${budgetHtml}
  ${donutHtml}
  ${trendHtml}
  ${recentHtml}
  ${total === 0 && count === 0 ? '<div class="empty">Nessun movimento in questo mese.<br>Tocca ➕ per aggiungere una spesa.</div>' : ''}
  `;
}

function rowExpenseHtml(e) {
  const isRec = e.type === 'RECURRING_INSTANCE';
  const meta = `${shortDate(e.date)}${e.note ? ' · ' + esc(e.note) : ''}${isRec ? ' · ricorrente' : ''}`;
  return `<div class="row" data-open-expense="${e.id}">
    <span class="dot" style="background:${colorForId(e.categoryId)}"></span>
    <div class="main"><div class="title">${esc(catName(e.categoryId))}</div><div class="meta">${meta}</div></div>
    <span class="amount">${Money.format(e.amountCents)}</span>
  </div>`;
}

// ---- Spese (spese + rate dovute) ---------------------------------------
function viewSpese() {
  const ym = speseMonth;
  const cats = activeCats();
  let rows = [];
  if (speseFilter !== 'INSTALLMENT') {
    expensesInMonth(ym).forEach(e => {
      if (speseFilter === 'SINGLE' && e.type !== 'SINGLE') return;
      if (speseFilter === 'RECURRING' && e.type !== 'RECURRING_INSTANCE') return;
      if (speseCatFilter && e.categoryId !== speseCatFilter) return;
      rows.push({ kind: e.type === 'RECURRING_INSTANCE' ? 'RECURRING' : 'SINGLE', id: e.id, date: e.date, amountCents: e.amountCents, categoryId: e.categoryId, note: e.note });
    });
  }
  if (speseFilter === 'ALL' || speseFilter === 'INSTALLMENT') {
    entriesDueInMonth(ym).forEach(en => {
      const p = planById(en.planId); if (!p) return;
      if (speseCatFilter && p.categoryId !== speseCatFilter) return;
      rows.push({ kind: 'INSTALLMENT', id: en.id, date: en.dueDate, amountCents: en.amountCents, categoryId: p.categoryId, note: p.title, info: `${en.number}/${p.count}`, paid: en.paid, planId: p.id });
    });
  }
  rows.sort((a, b) => (b.date < a.date ? -1 : b.date > a.date ? 1 : b.id - a.id));

  const chip = (val, label) => `<button class="chip ${speseFilter === val ? 'sel' : ''}" data-filter="${val}">${label}</button>`;
  const catChip = speseCatFilter ? `<button class="chip sel" data-catfilter="0">${esc(catName(speseCatFilter))} ✕</button>`
    : `<button class="chip" data-catmenu="1">Categoria ▾</button>`;

  const rowsHtml = rows.length ? `<div class="list">${rows.map(r => {
    const metaBits = [shortDate(r.date)];
    if (r.note) metaBits.push(esc(r.note));
    if (r.kind === 'RECURRING') metaBits.push('ricorrente');
    if (r.kind === 'INSTALLMENT') metaBits.push('rata ' + r.info);
    const openAttr = r.kind === 'INSTALLMENT' ? `data-open-plan="${r.planId}"` : `data-open-expense="${r.id}"`;
    const tag = r.kind === 'INSTALLMENT' ? `<span class="pill ${r.paid ? 'paid' : 'due'}">${r.paid ? 'pagata' : 'da pagare'}</span>` : '';
    return `<div class="row" ${openAttr}>
      <span class="dot" style="background:${colorForId(r.categoryId)}"></span>
      <div class="main"><div class="title">${esc(catName(r.categoryId))}</div><div class="meta">${metaBits.join(' · ')}</div></div>
      ${tag}<span class="amount">${Money.format(r.amountCents)}</span>
    </div>`;
  }).join('')}</div>` : '<div class="empty">Niente da mostrare in questo mese.</div>';

  return `
  <div class="appbar"><h1>Spese</h1></div>
  ${monthBar(ym, 'spese')}
  <div class="chips">
    ${catChip}
    ${chip('ALL', 'Tutte')}${chip('SINGLE', 'Singole')}${chip('RECURRING', 'Ricorrenti')}${chip('INSTALLMENT', 'Rate')}
  </div>
  ${rowsHtml}
  `;
}

// ---- Rate ---------------------------------------------------------------
function viewRate() {
  const plans = state.plans.slice().sort((a, b) => b.createdAt - a.createdAt);
  const body = plans.length ? `<div class="list">${plans.map(p => {
    const pr = planProgress(p.id);
    return `<div class="row" data-open-plan="${p.id}">
      <span class="dot" style="background:${colorForId(p.categoryId)}"></span>
      <div class="main"><div class="title">${esc(p.title)}</div>
      <div class="meta">${esc(catName(p.categoryId))} · ${pr.paid}/${pr.count} rate · ${Money.format(pr.paidCents)}/${Money.format(pr.total)}</div></div>
      <span class="amount">${Money.format(p.totalCents)}</span>
    </div>`;
  }).join('')}</div>` : '<div class="empty">Nessun piano rate.<br>Tocca "Nuovo piano".</div>';

  return `
  <div class="appbar"><h1>Rate</h1><button class="btn tonal" data-act="add-plan">Nuovo piano</button></div>
  ${body}`;
}

// ---- Impostazioni -------------------------------------------------------
function viewImpostazioni() {
  const t = state.settings.theme;
  const seg = (val, label) => `<button class="${t === val ? 'sel' : ''}" data-theme="${val}">${label}</button>`;
  const budget = state.settings.budgetCents || 0;
  const persisted = window.__persisted;
  return `
  <div class="appbar"><h1>Impostazioni</h1></div>
  <div class="section-label">Tema</div>
  <div class="seg">${seg('light', 'Chiaro')}${seg('dark', 'Scuro')}${seg('system', 'Sistema')}</div>

  <div class="section-label">Gestione</div>
  <div class="list">
    <div class="row" data-act="go-categorie"><div class="main"><div class="title">Categorie</div><div class="meta">Gestisci le categorie</div></div><span>›</span></div>
    <div class="row" data-act="go-ricorrenti"><div class="main"><div class="title">Spese ricorrenti</div><div class="meta">Regole mensili automatiche</div></div><span>›</span></div>
    <div class="row" data-act="edit-budget"><div class="main"><div class="title">Budget mensile</div><div class="meta">${budget > 0 ? 'Attuale: ' + Money.format(budget) : 'Non impostato'}</div></div><span>›</span></div>
  </div>

  <div class="section-label">Backup</div>
  <div class="banner">⚠️ Su iPhone i dati del browser possono essere cancellati dal sistema se non usi l'app per un po'. Esporta un backup ogni tanto.</div>
  <div class="list">
    <div class="row" data-act="export"><div class="main"><div class="title">Esporta dati (JSON)</div><div class="meta">Salva un backup di tutto</div></div><span>⬇️</span></div>
    <div class="row" data-act="import"><div class="main"><div class="title">Importa backup</div><div class="meta">Sostituisce i dati attuali</div></div><span>⬆️</span></div>
    <div class="row" data-act="reset"><div class="main"><div class="title">Reset dati</div><div class="meta">Cancella tutto e ripristina le categorie</div></div><span>🗑️</span></div>
  </div>

  <div class="hint">Archiviazione permanente: ${persisted === true ? 'attiva ✅' : persisted === false ? 'non garantita (fai backup)' : 'sconosciuta'}.</div>
  <div class="hint">Spese PWA · dati salvati solo su questo dispositivo. Per installare: su iPhone apri in Safari → Condividi → «Aggiungi a Home».</div>
  `;
}

// ---- Categorie ----------------------------------------------------------
function viewCategorie() {
  const rows = state.categories.slice().sort((a, b) => (a.sortOrder - b.sortOrder) || a.name.localeCompare(b.name)).map(c => {
    const used = categoryInUse(c.id);
    return `<div class="row">
      <span class="dot" style="background:${colorForId(c.id)}"></span>
      <div class="main"><div class="title">${esc(c.name)}${c.archived ? ' <span class="pill due">archiviata</span>' : ''}</div>
      <div class="meta">${used ? 'in uso' : 'non usata'}</div></div>
      <button class="iconbtn" data-rename-cat="${c.id}" aria-label="Rinomina">✏️</button>
      <button class="iconbtn" data-del-cat="${c.id}" aria-label="Elimina">🗑️</button>
    </div>`;
  }).join('');
  return `
  <div class="appbar"><button class="iconbtn" data-act="back-settings">‹</button><h1>Categorie</h1><button class="btn tonal" data-act="add-cat">Nuova</button></div>
  <div class="list">${rows}</div>
  <div class="hint">Una categoria in uso non si elimina: verrà archiviata (resta nello storico, sparisce dai menu).</div>`;
}

// ---- Ricorrenti ---------------------------------------------------------
function viewRicorrenti() {
  const rows = state.recurring.length ? state.recurring.map(r => `<div class="row" data-edit-rec="${r.id}">
    <span class="dot" style="background:${colorForId(r.categoryId)}"></span>
    <div class="main"><div class="title">${esc(r.title)}${r.active ? '' : ' <span class="pill due">disattiva</span>'}</div>
    <div class="meta">${esc(catName(r.categoryId))} · giorno ${r.dayOfMonth} · ${Money.format(r.amountCents)}</div></div>
    <span class="amount">${Money.format(r.amountCents)}</span>
  </div>`).join('') : '';
  return `
  <div class="appbar"><button class="iconbtn" data-act="back-settings">‹</button><h1>Ricorrenti</h1><button class="btn tonal" data-act="add-rec">Nuova</button></div>
  ${rows ? `<div class="list">${rows}</div>` : '<div class="empty">Nessuna spesa ricorrente.</div>'}
  <div class="hint">Le ricorrenti generano automaticamente una spesa al mese (anche nei mesi futuri, come previsione).</div>`;
}

// =========================================================================
//  MODALI
// =========================================================================
function openSheet(html, dialog = false) {
  $('#modal-root').innerHTML = `<div class="scrim" data-scrim="1"><div class="sheet ${dialog ? 'dialog' : ''}">${html}</div></div>`;
  return $('#modal-root .sheet');
}
function closeModal() { $('#modal-root').innerHTML = ''; }

function catOptions(selected) {
  return activeCats().map(c => `<option value="${c.id}" ${c.id === selected ? 'selected' : ''}>${esc(c.name)}</option>`).join('');
}

// ---- Aggiungi/Modifica spesa -------------------------------------------
function openExpenseSheet(id) {
  const e = id ? state.expenses.find(x => x.id === id) : null;
  const cats = activeCats();
  if (!cats.length) { openSheet(`<h2>Serve una categoria</h2><div class="hint">Crea prima una categoria in Impostazioni → Categorie.</div><div class="actions"><button class="btn text" data-scrim="1">Chiudi</button></div>`, true); return; }
  const isRec = e && e.type === 'RECURRING_INSTANCE';
  const sheet = openSheet(`
    <h2>${e ? 'Modifica spesa' : 'Nuova spesa'}</h2>
    ${isRec ? '<div class="hint">Istanza di una spesa ricorrente. Le modifiche valgono solo per questo mese.</div>' : ''}
    <div class="field"><label>Importo (€)</label><input id="f-amount" inputmode="decimal" placeholder="0,00" value="${e ? (e.amountCents / 100).toFixed(2).replace('.', ',') : ''}"><div class="err" id="e-amount" hidden>Importo non valido</div></div>
    <div class="field"><label>Categoria</label><select id="f-cat">${catOptions(e ? e.categoryId : cats[0].id)}</select></div>
    <div class="field"><label>Data</label><input id="f-date" type="date" value="${e ? e.date : todayISO()}"></div>
    <div class="field"><label>Note (facoltative)</label><input id="f-note" value="${e ? esc(e.note || '') : ''}"></div>
    <div class="actions">
      ${e ? '<button class="btn danger" data-del-expense="' + e.id + '">Elimina</button>' : ''}
      <button class="btn text" data-scrim="1">Annulla</button>
      <button class="btn" id="save-expense">Salva</button>
    </div>
  `);
  $('#save-expense', sheet).onclick = () => {
    const cents = Money.parse($('#f-amount', sheet).value);
    if (cents == null || cents <= 0) { $('#e-amount', sheet).hidden = false; return; }
    const date = $('#f-date', sheet).value || todayISO();
    const categoryId = Number($('#f-cat', sheet).value);
    const note = $('#f-note', sheet).value.trim() || null;
    if (e) { e.amountCents = cents; e.date = date; e.categoryId = categoryId; e.note = note; }
    else state.expenses.push({ id: nextId(), amountCents: cents, date, categoryId, note, type: 'SINGLE', recurringId: null });
    save(); closeModal(); render();
  };
}

// ---- Aggiungi piano rate -----------------------------------------------
function openPlanSheet() {
  const cats = activeCats();
  if (!cats.length) { openSheet(`<h2>Serve una categoria</h2><div class="hint">Crea prima una categoria.</div><div class="actions"><button class="btn text" data-scrim="1">Chiudi</button></div>`, true); return; }
  let mode = 'total'; // 'total' | 'interest'
  const sheet = openSheet(`
    <h2>Nuovo piano rate</h2>
    <div class="field"><label>Titolo</label><input id="p-title" placeholder="Es. iPhone, divano…"><div class="err" id="pe-title" hidden>Obbligatorio</div></div>
    <div class="seg" id="p-mode"><button class="sel" data-mode="total">Totale</button><button data-mode="interest">Con interessi</button></div>
    <div id="p-total-wrap">
      <div class="field"><label>Importo totale (€)</label><input id="p-total" inputmode="decimal" placeholder="0,00"></div>
    </div>
    <div id="p-interest-wrap" hidden>
      <div class="field"><label>Prezzo (€)</label><input id="p-price" inputmode="decimal" placeholder="0,00"></div>
      <div class="field"><label>Interessi (%)</label><input id="p-pct" inputmode="decimal" placeholder="0"></div>
      <div class="hint" id="p-calc">Totale calcolato: —</div>
    </div>
    <div class="field"><label>Numero rate</label><input id="p-count" inputmode="numeric" placeholder="12"><div class="err" id="pe-count" hidden>Minimo 1</div></div>
    <div class="field"><label>Prima scadenza</label><input id="p-first" type="date" value="${todayISO()}"></div>
    <div class="field"><label>Categoria</label><select id="p-cat">${catOptions(cats[0].id)}</select></div>
    <div class="field"><label>Note (facoltative)</label><input id="p-note"></div>
    <div class="actions"><button class="btn text" data-scrim="1">Annulla</button><button class="btn" id="save-plan">Crea</button></div>
  `);
  const recalc = () => {
    const price = Money.parse($('#p-price', sheet).value) || 0;
    const pct = parseFloat(($('#p-pct', sheet).value || '0').replace(',', '.')) || 0;
    const tot = Math.round(price * (1 + pct / 100));
    $('#p-calc', sheet).textContent = 'Totale calcolato: ' + Money.format(tot);
    return tot;
  };
  $$('#p-mode button', sheet).forEach(b => b.onclick = () => {
    mode = b.dataset.mode;
    $$('#p-mode button', sheet).forEach(x => x.classList.toggle('sel', x === b));
    $('#p-total-wrap', sheet).hidden = mode !== 'total';
    $('#p-interest-wrap', sheet).hidden = mode !== 'interest';
  });
  ['#p-price', '#p-pct'].forEach(s => { const el = $(s, sheet); if (el) el.oninput = recalc; });
  $('#save-plan', sheet).onclick = () => {
    const title = $('#p-title', sheet).value.trim();
    const count = parseInt($('#p-count', sheet).value, 10);
    const totalCents = mode === 'interest' ? recalc() : Money.parse($('#p-total', sheet).value);
    let ok = true;
    $('#pe-title', sheet).hidden = !!title; if (!title) ok = false;
    $('#pe-count', sheet).hidden = !(!count || count < 1); if (!count || count < 1) ok = false;
    if (totalCents == null || totalCents <= 0) ok = false;
    if (!ok) return;
    createPlan({ title, totalCents, count, firstDue: $('#p-first', sheet).value || todayISO(), categoryId: Number($('#p-cat', sheet).value), note: $('#p-note', sheet).value.trim() });
    closeModal(); render();
  };
}

// ---- Dettaglio piano ----------------------------------------------------
function openPlanDetail(id) {
  const p = planById(id); if (!p) return;
  const entries = state.entries.filter(e => e.planId === id).sort((a, b) => a.number - b.number);
  const pr = planProgress(id);
  const sheet = openSheet(`
    <h2>${esc(p.title)}</h2>
    <div class="hint">${esc(catName(p.categoryId))} · ${pr.paid}/${pr.count} rate pagate · residuo ${Money.format(pr.total - pr.paidCents)}</div>
    ${p.note ? `<div class="hint">${esc(p.note)}</div>` : ''}
    <div class="list" style="margin:8px 12px">${entries.map(en => `
      <div class="row">
        <div class="main"><div class="title">Rata ${en.number}</div><div class="meta">scad. ${shortDate(en.dueDate)}</div></div>
        <span class="amount">${Money.format(en.amountCents)}</span>
        <button class="chip ${en.paid ? 'sel' : ''}" data-toggle-entry="${en.id}">${en.paid ? '✓ pagata' : 'segna'}</button>
      </div>`).join('')}</div>
    <div class="actions">
      <button class="btn danger" data-del-plan="${p.id}">Elimina piano</button>
      <button class="btn text" data-scrim="1">Chiudi</button>
    </div>
  `);
}

// ---- Ricorrente edit ----------------------------------------------------
function openRecurringSheet(id) {
  const r = id ? state.recurring.find(x => x.id === id) : null;
  const cats = activeCats();
  if (!cats.length) { openSheet(`<h2>Serve una categoria</h2><div class="hint">Crea prima una categoria.</div><div class="actions"><button class="btn text" data-scrim="1">Chiudi</button></div>`, true); return; }
  const sheet = openSheet(`
    <h2>${r ? 'Modifica ricorrente' : 'Nuova ricorrente'}</h2>
    <div class="field"><label>Titolo</label><input id="r-title" value="${r ? esc(r.title) : ''}"><div class="err" id="re-title" hidden>Obbligatorio</div></div>
    <div class="field"><label>Importo (€)</label><input id="r-amount" inputmode="decimal" value="${r ? (r.amountCents / 100).toFixed(2).replace('.', ',') : ''}"><div class="err" id="re-amount" hidden>Importo non valido</div></div>
    <div class="field"><label>Categoria</label><select id="r-cat">${catOptions(r ? r.categoryId : cats[0].id)}</select></div>
    <div class="field"><label>Giorno del mese (1–28)</label><input id="r-day" inputmode="numeric" value="${r ? r.dayOfMonth : 1}"><div class="err" id="re-day" hidden>Tra 1 e 28</div></div>
    <div class="field"><label>Data inizio</label><input id="r-start" type="date" value="${r ? r.startDate : todayISO()}"></div>
    <div class="field"><label>Data fine (facoltativa)</label><input id="r-end" type="date" value="${r && r.endDate ? r.endDate : ''}"></div>
    <div class="field"><label><input type="checkbox" id="r-active" ${!r || r.active ? 'checked' : ''}> Attiva</label></div>
    <div class="actions">
      ${r ? '<button class="btn danger" data-del-rec="' + r.id + '">Elimina</button>' : ''}
      <button class="btn text" data-scrim="1">Annulla</button>
      <button class="btn" id="save-rec">Salva</button>
    </div>
  `);
  $('#save-rec', sheet).onclick = () => {
    const title = $('#r-title', sheet).value.trim();
    const cents = Money.parse($('#r-amount', sheet).value);
    const day = parseInt($('#r-day', sheet).value, 10);
    let ok = true;
    $('#re-title', sheet).hidden = !!title; if (!title) ok = false;
    $('#re-amount', sheet).hidden = !(cents == null || cents <= 0); if (cents == null || cents <= 0) ok = false;
    $('#re-day', sheet).hidden = !(!day || day < 1 || day > 28); if (!day || day < 1 || day > 28) ok = false;
    if (!ok) return;
    const categoryId = Number($('#r-cat', sheet).value);
    const startDate = $('#r-start', sheet).value || todayISO();
    const endDate = $('#r-end', sheet).value || null;
    const active = $('#r-active', sheet).checked;
    if (r) {
      Object.assign(r, { title, amountCents: cents, categoryId, dayOfMonth: day, startDate, endDate, active, lastGeneratedPeriod: null });
      syncRecurringInstances(r);
      removeFutureInstances(r.id);
    } else {
      state.recurring.push({ id: nextId(), title, amountCents: cents, categoryId, dayOfMonth: day, startDate, endDate, active, lastGeneratedPeriod: null });
    }
    save();
    if (active) generateRecurring();
    closeModal(); render();
  };
}

// ---- Budget dialog ------------------------------------------------------
function openBudgetDialog() {
  const cur = state.settings.budgetCents || 0;
  const sheet = openSheet(`
    <h2>Budget mensile</h2>
    <div class="hint">Limite di spesa per il mese (vuoto o 0 = nessun budget).</div>
    <div class="field"><label>Importo (€)</label><input id="b-amount" inputmode="decimal" value="${cur > 0 ? (cur / 100).toFixed(2).replace('.', ',') : ''}"></div>
    <div class="actions"><button class="btn text" data-scrim="1">Annulla</button><button class="btn" id="save-budget">Salva</button></div>
  `, true);
  $('#save-budget', sheet).onclick = () => {
    const cents = Money.parse($('#b-amount', sheet).value) || 0;
    state.settings.budgetCents = Math.max(0, cents); save(); closeModal(); render();
  };
}

// ---- Categoria dialog ---------------------------------------------------
function openCategoryDialog(id) {
  const c = id ? catById(id) : null;
  const sheet = openSheet(`
    <h2>${c ? 'Rinomina categoria' : 'Nuova categoria'}</h2>
    <div class="field"><label>Nome</label><input id="c-name" value="${c ? esc(c.name) : ''}"><div class="err" id="ce-name" hidden>Obbligatorio</div></div>
    <div class="actions"><button class="btn text" data-scrim="1">Annulla</button><button class="btn" id="save-cat">Salva</button></div>
  `, true);
  $('#save-cat', sheet).onclick = () => {
    const name = $('#c-name', sheet).value.trim();
    if (!name) { $('#ce-name', sheet).hidden = false; return; }
    if (c) c.name = name;
    else state.categories.push({ id: nextId(), name, archived: false, sortOrder: state.categories.length });
    save(); closeModal(); render();
  };
}

function confirmDialog(title, body, onYes, yesLabel = 'Conferma') {
  const sheet = openSheet(`<h2>${esc(title)}</h2><div class="hint">${esc(body)}</div>
    <div class="actions"><button class="btn text" data-scrim="1">Annulla</button><button class="btn danger" id="cd-yes">${esc(yesLabel)}</button></div>`, true);
  $('#cd-yes', sheet).onclick = () => { onYes(); closeModal(); render(); };
}

// =========================================================================
//  BACKUP
// =========================================================================
function exportJson() {
  const blob = new Blob([JSON.stringify(state, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = `spese-backup-${todayISO()}.json`;
  document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}
function importJson() {
  const inp = document.createElement('input');
  inp.type = 'file'; inp.accept = 'application/json,.json';
  inp.onchange = () => {
    const file = inp.files && inp.files[0]; if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const data = JSON.parse(reader.result);
        if (!data || !Array.isArray(data.categories)) throw new Error('Formato non valido');
        state = Object.assign(blankState(), data);
        load(); save(); render();
        alert('Backup importato.');
      } catch (err) { alert('Errore import: ' + err.message); }
    };
    reader.readAsText(file);
  };
  inp.click();
}

// =========================================================================
//  EVENTI (delegation)
// =========================================================================
document.addEventListener('click', (ev) => {
  const t = ev.target;

  // Bottom nav
  const nav = t.closest('[data-nav]');
  if (nav) { view = nav.dataset.nav; render(); return; }

  // Chiusura scrim / annulla
  if (t.dataset && t.dataset.scrim) { closeModal(); return; }
  if (t.id === 'modal-root') return;

  // FAB
  if (t.id === 'fab') { openExpenseSheet(null); return; }

  // Azioni con data-act
  const actEl = t.closest('[data-act]');
  if (actEl) {
    const act = actEl.dataset.act;
    if (act === 'dash-prev') { dashMonth = addMonthsYm(dashMonth, -1); render(); }
    else if (act === 'dash-next') { dashMonth = addMonthsYm(dashMonth, 1); render(); }
    else if (act === 'spese-prev') { speseMonth = addMonthsYm(speseMonth, -1); render(); }
    else if (act === 'spese-next') { speseMonth = addMonthsYm(speseMonth, 1); render(); }
    else if (act === 'add-plan') openPlanSheet();
    else if (act === 'go-categorie') { view = 'categorie'; render(); }
    else if (act === 'go-ricorrenti') { view = 'ricorrenti'; render(); }
    else if (act === 'back-settings') { view = 'impostazioni'; render(); }
    else if (act === 'edit-budget') openBudgetDialog();
    else if (act === 'add-cat') openCategoryDialog(null);
    else if (act === 'add-rec') openRecurringSheet(null);
    else if (act === 'export') exportJson();
    else if (act === 'import') importJson();
    else if (act === 'reset') confirmDialog('Azzerare tutti i dati?', 'Verranno eliminate spese, rate e ricorrenti. Le categorie tornano a quelle predefinite. Non reversibile.', () => {
      state = blankState(); seedIfEmpty(); save();
    }, 'Azzera');
    return;
  }

  // Tema
  const th = t.closest('[data-theme]');
  if (th) { state.settings.theme = th.dataset.theme; setTheme(th.dataset.theme); save(); render(); return; }

  // Filtri spese
  const fl = t.closest('[data-filter]'); if (fl) { speseFilter = fl.dataset.filter; render(); return; }
  const cf = t.closest('[data-catfilter]'); if (cf) { speseCatFilter = null; render(); return; }
  const cm = t.closest('[data-catmenu]'); if (cm) { openCatFilterMenu(); return; }

  // Aperture
  const oe = t.closest('[data-open-expense]'); if (oe) { openExpenseSheet(Number(oe.dataset.openExpense)); return; }
  const op = t.closest('[data-open-plan]'); if (op) { openPlanDetail(Number(op.dataset.openPlan)); return; }
  const er = t.closest('[data-edit-rec]'); if (er) { openRecurringSheet(Number(er.dataset.editRec)); return; }

  // Categorie manage
  const rc = t.closest('[data-rename-cat]'); if (rc) { openCategoryDialog(Number(rc.dataset.renameCat)); return; }
  const dc = t.closest('[data-del-cat]'); if (dc) { handleDeleteCategory(Number(dc.dataset.delCat)); return; }

  // Dentro le modali
  const de = t.closest('[data-del-expense]'); if (de) { const id = Number(de.dataset.delExpense); confirmDialog('Eliminare la spesa?', 'Operazione non reversibile.', () => { state.expenses = state.expenses.filter(x => x.id !== id); save(); }); return; }
  const dp = t.closest('[data-del-plan]'); if (dp) { const id = Number(dp.dataset.delPlan); confirmDialog('Eliminare il piano?', 'Verranno rimosse tutte le rate.', () => deletePlan(id)); return; }
  const dr = t.closest('[data-del-rec]'); if (dr) { const id = Number(dr.dataset.delRec); confirmDialog('Eliminare la ricorrente?', 'Le previsioni future verranno rimosse; lo storico resta.', () => { removeFutureInstances(id); state.recurring = state.recurring.filter(x => x.id !== id); save(); }); return; }
  const te = t.closest('[data-toggle-entry]'); if (te) { toggleEntry(Number(te.dataset.toggleEntry)); return; }
});

function toggleEntry(id) {
  const en = state.entries.find(x => x.id === id); if (!en) return;
  en.paid = !en.paid; en.paidDate = en.paid ? todayISO() : null; save();
  openPlanDetail(en.planId); // ridisegna il dettaglio
  render();
}
function handleDeleteCategory(id) {
  if (categoryInUse(id)) {
    confirmDialog('Archiviare la categoria?', 'È usata in alcuni movimenti, quindi non si elimina: verrà archiviata.', () => { const c = catById(id); if (c) c.archived = true; save(); }, 'Archivia');
  } else {
    confirmDialog('Eliminare la categoria?', 'Operazione non reversibile.', () => { state.categories = state.categories.filter(c => c.id !== id); save(); });
  }
}
function openCatFilterMenu() {
  const cats = activeCats();
  const sheet = openSheet(`<h2>Filtra per categoria</h2>
    <div class="list" style="margin:8px 12px">
      <div class="row" data-pickcat="0"><div class="main"><div class="title">Tutte le categorie</div></div></div>
      ${cats.map(c => `<div class="row" data-pickcat="${c.id}"><span class="dot" style="background:${colorForId(c.id)}"></span><div class="main"><div class="title">${esc(c.name)}</div></div></div>`).join('')}
    </div>`);
  $$('[data-pickcat]', sheet).forEach(el => el.onclick = () => { const v = Number(el.dataset.pickcat); speseCatFilter = v || null; closeModal(); render(); });
}

// =========================================================================
//  INIT
// =========================================================================
async function init() {
  load();
  seedIfEmpty();
  setTheme(state.settings.theme);
  generateRecurring();
  render();

  // Archiviazione persistente (riduce il rischio di eviction su iOS)
  try {
    if (navigator.storage && navigator.storage.persisted) {
      window.__persisted = await navigator.storage.persisted();
      if (!window.__persisted && navigator.storage.persist) {
        window.__persisted = await navigator.storage.persist();
      }
    }
  } catch (e) { /* ignore */ }

  // Service worker (offline). Fallisce in silenzio dove non supportato (es. sandbox).
  if ('serviceWorker' in navigator) {
    try { await navigator.serviceWorker.register('./sw.js'); } catch (e) { /* ignore */ }
  }
}
init();
