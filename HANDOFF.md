# HANDOFF — App "Spese"

> Invarianti architetturali, stack e comandi build sono in **CLAUDE.md** (caricato in automatico).
> Questo file = stato puntuale + conoscenza operativa per il prossimo agente.

## Goal
App Android nativa, 100% offline, per il tracking delle spese personali (Room + DataStore, Jetpack
Compose, MVVM + DI manuale). Il progetto è nato in un sandbox chat dove **non era possibile compilare**;
è stato portato qui per costruire e distribuire un **APK installabile e compatibile con Android 14**.

## Current Progress
**Roadmap 1–11 completa (MVP+).** L'app è funzionalmente completa e la build passa in CI.

Milestone: 1 Setup · 2 Model+Room · 3 Categorie · 4 Inserimento spesa · 5 Lista spese ·
6 Dashboard · 7 Rate · 8 Ricorrenti · 9 Widget · 10 Export/Import · 11 Rifinitura UX — **tutte ✅**.

Funzionalità presenti:
- **Spese singole**: form add/edit (importo→categoria→data→nota/metodo), lista mensile con navigazione
  mese, filtri categoria/tipo ("Tutte/Singole/Ricorrenti", scroll orizzontale), elimina con conferma.
- **Dashboard** (`feature/dashboard`): griglia 2×2 (Totale mese · N° movimenti · Rate aperte · Residuo rate),
  **budget mensile** con barra (rossa se sforato), **grafici Compose Canvas** (donut ripartizione categorie +
  "Andamento" a barre 6 mesi), ripartizione a barre colorate, ultime 5 spese. Totale mese =
  `sum(expense) + sumDue(installment)` (invarianti 4–6). "Rate aperte/Residuo" sono **globali** (non del mese).
- **Rate** (`feature/installments`): `InstallmentRepository.createPlan` (piano + rate in transazione unica
  `withTransaction`; `rata = floor(totale/n)`, l'ultima assorbe il resto). Form con due modalità: **"Totale"**
  (importo diretto, per piani già in corso) e **"Con interessi"** (`totale = prezzo × (1 + %/100)`, calcolato in
  `installmentTotalCents`, DB invariato). Lista con avanzamento, dettaglio con segna-pagata ed elimina (CASCADE).
- **Ricorrenti** (`feature/recurring`, da Impostazioni): `RecurringGenerator` **idempotente** materializza le
  istanze come `Expense(RECURRING_INSTANCE)` fino al mese corrente (dedup via `ExpenseDao.findRecurringInstance`,
  avanza `lastGeneratedPeriod`; rispetta start/end e `dayOfMonth` 1..28). Gira all'avvio e dopo create/edit.
- **Widget** (`feature/widget`, Glance): totale + n° movimenti del mese + **barra proporzioni per categoria**
  (Bitmap via Glance `Image`); tap → apre l'app. Provider `res/xml/spese_widget_info.xml` + receiver nel manifest.
- **Backup** (`core/backup` + `data/backup`, da Impostazioni): **Export JSON** (tutte le entità, SAF
  `CreateDocument`), **Import JSON** (`OpenDocument`, ripristino in transazione FK-safe, id preservati),
  **Export CSV** delle spese (`text/csv`, delimitatore `;`), **Reset** (svuota + riseminа categorie default).
- **Categorie**: CRUD + archiviazione (FK `RESTRICT`), 10 default al primo avvio, colore per-categoria
  derivato dall'id (`ui/theme/CategoryColors`, nessuna colonna DB).
- **Tema** chiaro/scuro/sistema (DataStore); **icona app** bar-chart arrotondato + layer `monochrome`.

## Build / distribuzione (CRITICO — leggere prima di toccare la build)
- **La build funziona SOLO via GitHub Actions**, non in locale in questo ambiente: `dl.google.com` è
  **bloccato dalla policy di egress** (403), quindi l'Android SDK non è scaricabile qui. Non aggirare il blocco.
- CI: `.github/workflows/build-apk.yml` → `assembleRelease` → carica artifact `spese-apk` **e** pubblica la
  Release **tag `latest`** con `app-release.apk`. Parte a ogni push su `main`/`master`/`claude/**`; i push di
  **sola `**.md` non innescano build** (`paths-ignore`).
- **APK**: release **non-debuggable** (fluido su mid-range) firmato con la **debug key**
  (`signingConfig = signingConfigs.getByName("debug")`) → installabile direttamente. **R8/minify ATTIVO**
  (`isMinifyEnabled = true`), keep-rule in `app/proguard-rules.pro`. minSdk 26 / target 35 → **compatibile Android 14**.
- Download utente: GitHub → *Releases* → `latest` → `app-release.apk`
  (`https://github.com/giacomorossetto03/App-spese-claude/releases/tag/latest`).
- Monitorare la CI con i tool MCP `mcp__github__actions_list/get_job_logs` (owner `giacomorossetto03`,
  repo `App-spese-claude`). Le risposte `list_workflow_runs` sono enormi → salvarle e leggere i campi con python.
- Branch di lavoro: **`claude/android-14-apk-build-zjr5s7`**.

## What Worked
- **CI su GitHub Actions** come unico percorso di build (SDK preinstallato sui runner) + pubblicazione automatica
  in Release: dà all'utente un `.apk` con link stabile senza SDK locale.
- Package-by-feature + **DI manuale** (`AppContainer` + `viewModelFactory{}`): pulito, zero boilerplate.
- Room source of truth con `Flow` → ViewModel `StateFlow<UiState>` → Compose; filtri via `flatMapLatest`;
  aggregati con `combine` (annidato quando servono >5 sorgenti: `Triple`/data class `DashboardAux`).
- **Grafici senza librerie esterne** (Compose Canvas per dashboard, Bitmap+Glance `Image` per il widget):
  niente nuove dipendenze, niente rischi di risoluzione Maven.
- Interessi/budget **senza toccare lo schema DB** (calcolo nel ViewModel / DataStore) → nessuna perdita dati
  su `fallbackToDestructiveMigration`.
- Milestone incrementali con verifica build ad ogni push; feature rischiose (widget, R8) isolate in commit propri.

## What Didn't Work (errori già risolti — NON ripeterli)
- **Build locale impossibile** (SDK assente, `dl.google.com`/Maven bloccati). Usare SEMPRE la CI.
- `Material3.ExposedDropdownMenu` **non è top-level** (è membro di `ExposedDropdownMenuBoxScope`): non importarlo,
  si risolve via scope dentro `ExposedDropdownMenuBox`.
- Glance: `actionStartActivity<MainActivity>()` reified **non si risolveva** → usare
  `actionStartActivity(Intent(context, MainActivity::class.java))`.
- `rememberScrollState` è in `androidx.compose.foundation`, **non** `.runtime`.
- Icone `Icons.AutoMirrored.Outlined.*` non tutte esistono (es. `ReceiptLong`): usare varianti sicure
  (`Payments`, `Receipt`, `CalendarMonth`, `Savings`).
- Non dimenticare import ovvi in file lunghi (`Box`, `height`, geometrie Canvas).
- **Bug corretto**: modificare un'istanza `RECURRING_INSTANCE` dalla lista la forzava a `SINGLE` azzerando
  `recurringId` → duplicato alla generazione. Ora `AddEditExpenseViewModel` conserva tipo e `recurringId`.
- **Bug grafico corretto**: doppio padding status bar sulle sotto-schermate → risolto con
  `AppRoot … consumeWindowInsets(padding)`.
- `gradle-wrapper.jar` è binario ed è **già incluso**: non rigenerarlo.

## Next Steps
1. **Verificare l'APK release sul device** (Oppo Reno 7): app si apre, tutte le schermate ok, e soprattutto
   **Backup Export/Import JSON** (parte più sensibile a R8). Se R8 causa crash a runtime: rollback immediato
   con `isMinifyEnabled = false` in `app/build.gradle.kts` e ripubblicare.
2. **Migrazioni Room vere**: oggi `exportSchema = false` + `fallbackToDestructiveMigration` (invariante 11).
   Quando lo schema si considera stabile, passare a `exportSchema = true` + migrazioni per non perdere i dati
   utente ad ogni cambio schema.
3. **Idee future opzionali** (non in roadmap): notifiche scadenze rate; budget **per categoria** (l'entità
   `MonthlyBudget` è già in schema, per-periodo/categoria); ricerca nelle spese; andamento a 12 mesi;
   export CSV anche di rate/ricorrenti.

## Come continuare
Sviluppare sul branch `claude/android-14-apk-build-zjr5s7`, committare e **pushare**: la CI ricompila e
aggiorna la Release `latest`. Verificare l'esito via tool MCP GitHub prima di considerare fatto.
