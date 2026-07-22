# HANDOFF — App "Spese"

> Invarianti, stack e comandi build in **CLAUDE.md** (caricato in automatico da Claude Code).
> Questo file = stato puntuale + prossimi passi.

## Goal
App Android nativa, 100% offline, per il tracking delle spese personali. Obiettivo immediato:
proseguire le milestone verso l'MVP potendo ora **compilare e testare in locale** (cosa non
possibile nel sandbox chat dove il progetto è nato).

## Current Progress
Milestone **1–5 complete** e presenti nel repo:
- **M1 Setup** — Gradle (version catalog), tema Material 3 chiaro/scuro, bottom nav 4 voci + FAB, scaffold navigazione.
- **M2 Model + Room** — 6 entità, 5 DAO con query aggregate mese/rate/ricorrenti già scritte.
- **M3 Categorie** — CRUD, seed 10 categorie default al primo avvio, archiviazione; selettore tema persistito su DataStore.
- **M4 Inserimento spesa singola** — form add/edit (importo→categoria→data→nota/metodo), validazione, salvataggio.
- **M5 Lista spese** — elenco del mese, navigazione mese (◀▶), filtri categoria/tipo, tap→modifica, elimina con conferma.

Incluso: **Gradle wrapper** completo (`gradlew` + `gradle-wrapper.jar`) e **CI** `.github/workflows/build-apk.yml`
che genera l'APK come artifact. ~47 file Kotlin. Coerenza package/path verificata, 0 import interni irrisolti.

## What Worked
- Package-by-feature + DI manuale (AppContainer + `viewModelFactory{}`): pulito, zero boilerplate.
- Room source of truth con `Flow`; ViewModel `StateFlow<UiState>`; filtri lista via `flatMapLatest`.
- Batch M1+M2+M3 in un'unica passata (autorizzato) per risparmiare token.
- Wrapper Gradle recuperato da GitHub raw quando serviva il binario.

## What Didn't Work
- **Build/APK impossibile nel sandbox chat**: Android SDK assente e repository Google Maven /
  Gradle / Maven Central bloccati (403). Da lì non si compila → per questo si è passati a Claude Code in locale.
- `gradle-wrapper.jar` è binario ed è **già incluso**: non rigenerarlo salvo necessità.

## Next Steps — Milestone 6: Dashboard (SOLO sorgente Expense)
Le rate entrano in M7; in M6 la dashboard usa solo `expense`.
1. `DashboardRepository`: esporre per il mese selezionato, combinando i Flow (usare `Dates.monthBounds(ym)`):
   - totale mese → `ExpenseDao.sumInMonth`
   - n° spese → `ExpenseDao.countInMonth`
   - ripartizione categorie → `ExpenseDao.sumByCategoryInMonth` (map `categoryId`→nome via `CategoryRepository`)
   - ultime spese → `ExpenseDao.observeRecent(5)`
2. `feature/dashboard`: `DashboardUiState`, `DashboardViewModel` (mese corrente + prev/next opzionale),
   `DashboardScreen` che sostituisce il placeholder.
3. UI (design già deciso): griglia 2×2 `SummaryCard` (**Totale mese · N° spese · Rate aperte · Residuo rate**);
   in M6 le due card "Rate" restano a 0/placeholder (si popolano in M7). Sotto: **Ripartizione categorie**
   (barre testuali semplici) + **Ultime spese** (5 righe, riuso stile riga di `ExpensesScreen`).
4. `AppRoot`: `composable(Routes.DASHBOARD){ DashboardScreen() }`. Tap su una spesa → `Routes.addEdit(id=...)` (opzionale).
5. Verifica: `./gradlew compileDebugKotlin` → `assembleDebug` → installare e controllare i totali con qualche spesa.

**Attenzione:** denaro `Long` in centesimi; **NON** introdurre le rate nel calcolo M6 (arrivano in M7 come
`sum(expense) + sumDue(installment)`); mantenere coerenza totale/conteggio.

Dopo la M6 → **M7 Rate**: `InstallmentRepository.createPlan` (transazione unica plan+entries, `floor` e ultima
rata che assorbe il resto) e unione nella dashboard. `RecurringGenerator` è la M8.
