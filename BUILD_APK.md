# Ottenere l'APK senza Android Studio

L'app è compilabile solo con **JDK 17 + Android SDK da riga di comando**: non serve l'IDE.
Sotto due strade. La (A) non richiede nulla installato sul tuo PC.

## A) GitHub Actions — APK pronto da scaricare (consigliata)
1. Crea un repo su GitHub e carica questa cartella (inclusa `.github/`).
   ```bash
   git init && git add . && git commit -m "Spese app"
   git branch -M main
   git remote add origin https://github.com/<tuo-utente>/<repo>.git
   git push -u origin main
   ```
2. Su GitHub apri **Actions** → il workflow *Build APK* parte da solo (o "Run workflow").
3. A fine build apri la run → sezione **Artifacts** → scarica **spese-debug-apk**.
   Dentro trovi `app-debug.apk`.

La build gira sui runner di GitHub (che hanno l'Android SDK): tu non installi niente.

## B) Build locale da terminale (senza IDE)
Prerequisiti: **JDK 17** e le *Android command-line tools*.
```bash
# 1. SDK minimo (esempio Linux/macOS)
export ANDROID_HOME="$HOME/android-sdk"
sdkmanager --sdk_root="$ANDROID_HOME" \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"
yes | sdkmanager --licenses

# 2. Compila
./gradlew assembleDebug        # su Windows: gradlew.bat assembleDebug
```
APK generato in:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Installare l'APK sul telefono
- È un **APK debug**, già firmato con la debug key: installabile direttamente.
- Trasferiscilo sul telefono (USB, cloud, email) e aprilo, oppure via cavo:
  ```bash
  adb install app-debug.apk
  ```
- Attiva **"Installa app sconosciute"** per l'app da cui apri il file (Impostazioni → App).

> Nota: è una build di debug per uso personale. Per una release firmata servirebbe un
> keystore tuo e `assembleRelease` (non necessario per installartela da solo).
