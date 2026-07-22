# HANDOFF — App "Spese"

> Invarianti, stack e comandi build in **CLAUDE.md** (caricato in automatico da Claude Code).
> Questo file = stato puntuale + prossimi passi.

## Goal
App Android nativa, 100% offline, per il tracking delle spese personali. Obiettivo immediato:
proseguire le milestone verso l'MVP potendo ora **compilare e testare in locale** (cosa non
possibile nel sandbox chat dove il progetto è nato).

## Current Progress
Milestone **1–7 complete** e presenti nel repo:
- **M1 Setup** — Gradle (version catalog), tema Material 3 chiaro/scuro, bottom nav 4 voci + FAB, scaffold navigazione.
- **M2 Model + Room** — 6 entità, 5 DAO con query aggregate mese/rate/ricorrenti già scritte.
- **M3 Categorie** — CRUD, seed 10 categorie default al primo avvio, archiviazione; selettore tema persistito su DataStore.
- **M4 Inserimento spesa singola** — form add/edit (importo→categoria→data→nota/metodo), validazione, salvataggio.
- **M5 Lista spese** — elenco del mese, navigazione mese (◀▶), filtri categoria/tipo, tap→modifica, elimina con conferma.
- **M6 Dashboard (solo Expense)** — `DashboardRepository` (totale/conteggio/ripartizione categorie/ultime spese),
  `DashboardViewModel` (mese + prev/next, `combine` dei Flow), `DashboardScreen` (griglia 2×2, barre categorie,
  ultime 5 spese con tap→modifica).
- **M7 Rate (+ unione dashboard)** — `InstallmentRepository.createPlan` (piano + rate in transazione unica via
  Room `withTransaction`; `rata = floor(totale/n)`, ultima assorbe il resto; scadenze mensili). UI: lista piani
  con avanzamento, creazione piano (anteprima rata), dettaglio con segna-pagata ed elimina (CASCADE). Dashboard
  unita: `totale = sum(expense) + sumDue(installment)`, ripartizione fusa, card "Rate aperte"/"Residuo rate"
  reali, card conteggio rinominata "N° movimenti" (spese + rate dovute) per coerenza col totale.

Incluso: **Gradle wrapper** completo (`gradlew` + `gradle-wrapper.jar`) e **CI** `.github/workflows/build-apk.yml`
che compila l'APK, lo carica come artifact **e** lo pubblica come **Release** (tag mobile `debug-latest`,
link diretto al file `.apk`). ~50 file Kotlin.

### Build / APK — stato verificato
- **Compila e passa su GitHub Actions** (runner con Android SDK preinstallato). APK debug: minSdk 26 / target 35
  → installabile e **compatibile con Android 14**.
- In ambienti con egress ristretto (es. Claude Code su web) `dl.google.com` è **bloccato da policy**: l'SDK non
  è scaricabile in locale → **usare la CI** (è il percorso di build supportato). Non aggirare il blocco.
- **Bug M4 corretto**: `AddEditExpenseScreen.kt` importava `androidx.compose.material3.ExposedDropdownMenu`, che
  in Material3 1.3.x **non è un simbolo top-level** ma un metodo di `ExposedDropdownMenuBoxScope` → "Unresolved
  reference". Import rimosso; la chiamata dentro `ExposedDropdownMenuBox` si risolve via scope.

## What Worked
- Package-by-feature + DI manuale (AppContainer + `viewModelFactory{}`): pulito, zero boilerplate.
- Room source of truth con `Flow`; ViewModel `StateFlow<UiState>`; filtri lista via `flatMapLatest`.
- Batch M1+M2+M3 in un'unica passata (autorizzato) per risparmiare token.
- Wrapper Gradle recuperato da GitHub raw quando serviva il binario.

## What Didn't Work
- **Build/APK impossibile nel sandbox chat**: Android SDK assente e repository Google Maven /
  Gradle / Maven Central bloccati (403). Da lì non si compila → per questo si è passati a Claude Code in locale.
- `gradle-wrapper.jar` è binario ed è **già incluso**: non rigenerarlo salvo necessità.

## Next Steps — Milestone 8: Ricorrenti
Le ricorrenti vanno **materializzate** come `Expense(type=RECURRING_INSTANCE)` così il totale resta
`sum(expense) + sumDue(installment)` senza doppi conteggi (invariante 5).
1. `RecurringRepository` sul `RecurringExpenseDao` (query `toGenerate`, `lastGeneratedPeriod` YYYYMM già pronte):
   CRUD delle regole ricorrenti (importo, categoria, `dayOfMonth` 1..28, nota/metodo).
2. `RecurringGenerator`: alla data odierna (o all'avvio) genera le istanze mancanti fino al mese corrente,
   creando `Expense(type=RECURRING_INSTANCE, recurringId=…)` con `ExpenseDao.findRecurringInstance` per evitare
   duplicati; aggiornare `lastGeneratedPeriod`. Idempotente. Data istanza = `dayOfMonth` del periodo.
3. UI `feature/recurring` (nuova) o sezione in Impostazioni: elenco/gestione regole. Le istanze appaiono già in
   lista spese e dashboard (sono `Expense`), con etichetta "ricorrente" già gestita in `ExpensesScreen`.

**Attenzione:** niente nuovi calcoli nel totale (le istanze SONO `Expense`); il generatore dev'essere idempotente.
Dopo M8 → M9 Widget (Glance), M10 Export/Import (kotlinx.serialization), M11 UX.

## Come ottenere l'APK
- **Release (consigliato)**: GitHub → *Releases* → `debug-latest` → scarica `app-debug.apk`. Aggiornato a ogni push.
- **Artifact**: Actions → ultima run *Build APK* → *Artifacts* → `spese-debug-apk` (zip da estrarre).
- Ogni push su `main`/`master`/`claude/**` ricompila e ripubblica automaticamente.
