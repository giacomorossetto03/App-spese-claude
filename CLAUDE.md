# CLAUDE.md — App "Spese" (expense tracker Android)

Contesto persistente per Claude Code. Leggere **HANDOFF.md** per stato puntuale e prossimi passi.

## Ruolo e stile di lavoro
- Utente = product owner. Tu = senior Android engineer.
- Lingua: **italiano**. Conciso ma preciso.
- **No over-engineering, niente feature extra non richieste.** Incrementale e disciplinato:
  una milestone alla volta (batch solo se l'utente lo autorizza). Prima piano, poi codice.

## L'app
Android nativa per il tracking delle spese personali. **100% offline**: nessun backend,
login o cloud. Tutti i dati sul dispositivo (Room + DataStore).

## Stack
Kotlin · Jetpack Compose (Material 3) · Room · DataStore · MVVM + Repository ·
Navigation Compose · **DI manuale** (AppContainer + `viewModelFactory{}`, niente Hilt).
Predisposte Glance (widget, M9) e kotlinx.serialization (export, M10).
Versioni nel version catalog `gradle/libs.versions.toml`: Kotlin 2.0.21, AGP 8.7.3,
KSP 2.0.21-1.0.28, Compose BOM 2024.12.01, Room 2.6.1 · minSdk 26 · compile/target 35 · Java 17.

## Invarianti architetturali (NON derogare)
1. Room è la **source of truth**, espone `Flow` → ViewModel (`StateFlow<UiState>`) → Compose.
2. **Denaro = `Long` in centesimi**, sempre. Mai `Double`. Formattazione `Money.format`, parsing `Money.parseToCents`.
3. Date: `LocalDate` ↔ **epochDay (`Long`)** nel DB. Periodo mese: `YearMonth` ↔ **Int YYYYMM**.
   Range mese half-open `[primo giorno, primo del mese dopo)` via `Dates.monthBounds(ym)`.
4. **Le rate NON sono `Expense`**: vivono in `installment_entry`. La dashboard (M7) unisce due
   sorgenti: `expense` + rate **dovute** nel mese (pagate o no).
5. Totale mese = spese singole + rate dovute nel mese + istanze ricorrenti del mese.
   Le ricorrenti sono **materializzate** come `Expense(type=RECURRING_INSTANCE)`, quindi
   `totale = sum(expense) + sumDue(installment)` (nessun doppio conteggio).
6. N° spese mese e ripartizione categorie **coerenti** col totale.
7. Arrotondamento rate: `rata = floor(totale/n)`, l'ultima assorbe il resto.
8. Categoria in uso non si cancella: `isArchived`. FK categoria `onDelete = RESTRICT`.
9. In DB **solo primitivi** (niente TypeConverter): enum come `String`, conversione nei mapper.
10. Package-by-feature, singolo modulo `:app`, package base `com.personal.spese`.
11. `exportSchema = false` + `fallbackToDestructiveMigration` finché lo schema non si stabilizza.
    Quando si stabilizza → passare a migrazioni vere.

## Struttura
```
core/db/{entity,dao} + AppDatabase · core/model · core/util(Money,Dates) · core/datastore
data/{mapper,repository} · di · ui/theme · ui/components · navigation
feature/{dashboard,expenses,installments,settings,categories}
```

## Modello dati (6 entità)
`Category` · `Expense`(type SINGLE|RECURRING_INSTANCE, `recurringId` solo riferimento logico) ·
`InstallmentPlan`(nessun contatore aggregato) · `InstallmentEntry`(FK CASCADE su plan) ·
`RecurringExpense`(dayOfMonth 1..28, `lastGeneratedPeriod` YYYYMM) · `MonthlyBudget`(post-MVP).
I DAO hanno già pronte le query aggregate: `sumInMonth`, `countInMonth`, `sumByCategoryInMonth`,
`observeRecent`, `observeFiltered`, `observePlansProgress`, `sumDueInMonth`, `countDueInMonth`,
`sumDueByCategoryInMonth`, `nextUpcoming`, `toGenerate`.

## Build / test (senza Android Studio)
Prerequisiti: JDK 17 + Android cmdline-tools, `ANDROID_HOME` impostato, poi una volta:
```
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0" && yes | sdkmanager --licenses
```
Comandi:
```
./gradlew compileDebugKotlin     # check rapido di compilazione
./gradlew assembleDebug          # APK -> app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
In alternativa la CI `.github/workflows/build-apk.yml` produce l'APK come artifact.

## Convenzioni
- Composable snelli; niente logica di dominio nei Composable. Stato in ViewModel come `StateFlow<UiState>`.
- I Composable non vedono le `*Entity`: i repository mappano entity ↔ domain.
- Testi UI in italiano.

## Roadmap
1 Setup ✅ · 2 Model+Room ✅ · 3 Categorie ✅ · 4 Inserimento spese ✅ · 5 Lista spese ✅ ·
6 Dashboard (solo Expense) ✅ · 7 Rate (+unione dashboard) ✅ · 8 Ricorrenti ✅ ·
**9 Widget ⏭ PROSSIMA** · 10 Export/Import · 11 Rifinitura UX.
