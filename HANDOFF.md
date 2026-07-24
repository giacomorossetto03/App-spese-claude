# HANDOFF — App "Spese"

> Invarianti, stack e comandi build in **CLAUDE.md** (caricato in automatico da Claude Code).
> Questo file = stato puntuale + prossimi passi.

## Goal
App Android nativa, 100% offline, per il tracking delle spese personali. Obiettivo immediato:
proseguire le milestone verso l'MVP potendo ora **compilare e testare in locale** (cosa non
possibile nel sandbox chat dove il progetto è nato).

## Current Progress
Milestone **1–9 complete** e presenti nel repo:
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
- **M8 Ricorrenti** — `RecurringRepository` (CRUD + `getById`) e `RecurringGenerator` **idempotente** che
  materializza le istanze mancanti fino al mese corrente come `Expense(RECURRING_INSTANCE)` (via
  `ExpenseDao.findRecurringInstance`, avanzando `lastGeneratedPeriod` YYYYMM; rispetta start/end e `dayOfMonth`
  1..28). Eseguito all'avvio (`AppContainer`) e dopo create/edit/riattivazione. UI `feature/recurring` (elenco +
  form) da Impostazioni → "Spese ricorrenti". Nessun nuovo calcolo in dashboard/lista: le istanze SONO `Expense`.
- **M9 Widget (Glance)** — `feature/widget`: `SpeseWidget` (GlanceAppWidget) + `SpeseWidgetReceiver`, provider
  `res/xml/spese_widget_info.xml`, receiver nel manifest. Mostra totale + n° movimenti del mese (letti dal DB via
  `.first()` sui Flow dei DAO); usa `GlanceTheme` (Material 3); tap → apre l'app via `Intent(context, MainActivity)`.

### Extra fuori-roadmap (richieste utente)
- **Rate con interessi** (`feature/installments/addplan`): due modalità nel form — "Totale" (importo diretto,
  per piani già in corso: si scrive il residuo) e "Con interessi" (prezzo × (1 + %/100)). Totale calcolato nel
  ViewModel (`installmentTotalCents`); `createPlan` invariato → **nessuna modifica allo schema DB**.
- **Restyling**: schema colori M3 con container; `ui/theme/CategoryColors` (colore per categoria dall'id, no DB);
  dashboard con card+icone, card totale in primaryContainer, barre categoria colorate; pallini colore nelle liste.
- **Fix pulsanti**: segmentati Ricorrente/Rateizzata nel form spesa → scorciatoie ai flussi dedicati.

Incluso: **Gradle wrapper** completo (`gradlew` + `gradle-wrapper.jar`) e **CI** `.github/workflows/build-apk.yml`
che compila l'APK **release**, lo carica come artifact **e** lo pubblica come **Release** (tag mobile `latest`,
link diretto al file `.apk`). ~60 file Kotlin.

### Rifinitura (fix + performance)
- **Fix pulsanti**: nel form spesa i segmentati "Ricorrente"/"Rateizzata" erano disabilitati (dead-end). Ora, in
  inserimento, sono scorciatoie ai flussi dedicati (M8/M7); nascosti in modifica. Export/Import/Reset in
  Impostazioni restano la M10 (disabilitati, etichettati).
- **Anti-scatti**: la CI ora compila **`assembleRelease`** (non-debuggable → molto più fluido su mid-range),
  firmato con la debug key (`signingConfig = signingConfigs.getByName("debug")`) → installabile direttamente.
  `minify` off per sicurezza (R8 = ottimizzazione futura facoltativa). Asset: `app-release.apk`, tag `latest`.

### Build / APK — stato verificato
- **Compila e passa su GitHub Actions** (runner con Android SDK preinstallato). APK release: minSdk 26 / target 35
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

## Next Steps — Milestone 10: Export/Import (kotlinx.serialization)
`kotlinx-serialization-json` già in dipendenze; in Impostazioni ci sono già le voci disabilitate
(Export / Import / Reset) da abilitare.
1. **Export**: serializzare tutte le entità (categorie, spese, piani+rate, ricorrenti) in un JSON (e opz. CSV
   delle spese). Salvataggio via Storage Access Framework (`ActivityResultContracts.CreateDocument`).
2. **Import**: leggere il JSON (`OpenDocument`), validare, e ripristinare in transazione (attenzione a id/FK:
   reinserire con id azzerati e rimappare le referenze, oppure svuotare e reimportare).
3. **Reset dati**: conferma forte → `clearAllTables()` (o cancellazioni ordinate rispettando le FK RESTRICT).
4. Collegare i 3 `SettingRow` oggi `enabled = false`.

**Attenzione:** con `fallbackToDestructiveMigration` non cambiare lo schema senza motivo (perdita dati sul device
dell'utente). Poi M11 rifinitura UX. Nota: R8/minify è **off** nel release — abilitabile come ottimizzazione.

## Come ottenere l'APK
- **Release (consigliato)**: GitHub → *Releases* → `latest` → scarica `app-release.apk` (release ottimizzata,
  installabile). Aggiornato a ogni push di codice.
- **Artifact**: Actions → ultima run *Build APK* → *Artifacts* → `spese-apk`.
- I push di sola documentazione (`**.md`) non fanno partire la CI. Il vecchio tag `debug-latest` (APK debug) è superato.
