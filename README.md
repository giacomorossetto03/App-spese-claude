# Spese — expense tracker Android (100% offline)

App Android nativa per il tracking delle spese personali. Nessun backend, login o cloud:
tutti i dati restano sul dispositivo (Room + DataStore).

## Stack
Kotlin · Jetpack Compose (Material 3) · Room · DataStore · MVVM + Repository ·
Navigation Compose · DI manuale (AppContainer). Predisposte Glance (widget) e
kotlinx.serialization (export) per le milestone successive.

## Requisiti
- Android Studio (Ladybug o successivo)
- JDK 17
- Android SDK 35, dispositivo/emulatore con **Android 8.0 (API 26)** o superiore

## Come aprire ed eseguire
1. Android Studio → **Open** → seleziona questa cartella.
2. Al primo import, Studio scarica Gradle 8.9 e sincronizza (serve connessione).
3. **Run 'app'** su emulatore o dispositivo.

> Nota: il binario `gradle/wrapper/gradle-wrapper.jar` non è incluso (è un file
> binario). Android Studio lo rigenera in automatico all'apertura. In alternativa,
> da terminale con Gradle installato: `gradle wrapper` e poi `./gradlew assembleDebug`.

## APK senza Android Studio
Vedi **BUILD_APK.md**: build via GitHub Actions (APK scaricabile, zero setup locale)
oppure via `./gradlew assembleDebug` da terminale con le command-line tools.

## Assunzioni prese
- Package/applicationId: `com.personal.spese`, nome app "Spese".
- `minSdk 26` per usare `java.time` nativo (niente desugaring) e icone adaptive XML (niente PNG).
- `exportSchema = false` + `fallbackToDestructiveMigration` finché lo schema non si stabilizza.
- Denaro sempre in **centesimi (Long)**; date come **epochDay (Long)**; periodo mese come **Int YYYYMM**.

## Architettura (decisioni portanti)
- Room è la source of truth (espone `Flow`) → ViewModel (`StateFlow`) → Compose.
- Le **rate NON sono spese**: vivono in `installment_entry`. La dashboard (milestone 7)
  unirà due sorgenti: `expense` + rate **dovute** nel mese.
- Le ricorrenti verranno **materializzate** come `expense` (type `RECURRING_INSTANCE`),
  così la somma del mese resta uniforme.

## Struttura
```
app/src/main/java/com/personal/spese/
├─ core/db/        entità, DAO, AppDatabase
├─ core/model/     modelli di dominio, enum, proiezioni query
├─ core/util/      Money, Dates
├─ core/datastore/ SettingsDataStore (tema)
├─ data/           mapper + repository
├─ di/             AppContainer + factory ViewModel
├─ ui/theme/       Material 3 (chiaro/scuro, 1 accento)
├─ ui/components/  componenti condivisi
├─ navigation/     rotte + scaffold (bottom bar + FAB)
└─ feature/        dashboard · expenses · installments · settings · categories
```

## Stato milestone
- [x] **M1 Setup** — build, tema, navigazione, scaffold
- [x] **M2 Model + Room** — 6 entità, 5 DAO (query mese/rate/ricorrenti già pronte)
- [x] **M3 Categorie** — CRUD, seed default, archiviazione; tema chiaro/scuro persistente
- [x] **M4 Inserimento spese** — form Nuova/Modifica spesa singola (importo, categoria, data, nota, metodo)
- [x] **M5 Lista spese** — elenco del mese, filtri (mese ◀▶ / categoria / tipo), tap→modifica, elimina
- [ ] M6 Dashboard · M7 Rate · M8 Ricorrenti
      · M9 Widget · M10 Export/Import · M11 Rifinitura

## Cosa è già funzionante
Bottom nav a 4 voci, tre schermate placeholder, **Impostazioni** con selettore tema
funzionante, **Categorie** completa (aggiungi / rinomina / archivia-ripristina) con
10 categorie predefinite seed-ate al primo avvio.
