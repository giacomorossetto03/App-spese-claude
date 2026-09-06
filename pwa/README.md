# Spese — PWA (versione web installabile)

Versione **Progressive Web App** dell'app Spese: stesse funzioni dell'app Android, ma gira nel
browser, **funziona offline** e si può **installare su iPhone e Android** (icona in home, schermo intero).
Nessun backend: i dati stanno **solo sul dispositivo** (localStorage del browser).

## Funzioni
- Spese singole (add/edit/elimina), navigazione per mese, filtri categoria/tipo.
- Rate: piano con **totale** o **con interessi** (`totale = prezzo × (1 + %/100)`); segna pagata; elimina.
- Ricorrenti: regola mensile, materializzazione automatica + **previsioni** fino a 12 mesi.
- Dashboard: totale/movimenti del mese, rate aperte/residuo, **budget**, donut categorie, andamento 6 mesi, ultime spese.
- La lista Spese unisce spese + **rate dovute nel mese** (come nell'app Android).
- Tema chiaro/scuro/sistema. Backup **Export/Import JSON**. Reset.

Denaro sempre in **centesimi** (interi). Date in `YYYY-MM-DD`, periodo mese `YYYY-MM`.

## Struttura
```
index.html · styles.css · app.js · manifest.webmanifest · sw.js · icons/
gen-icons.cjs  (rigenera le icone PNG con Playwright)
test.cjs       (smoke test end-to-end con Playwright)
```

## Provare in locale
```
npx http-server pwa -p 8080     # oppure: python3 -m http.server 8080 --directory pwa
# apri http://localhost:8080
```

## Pubblicare (per installarla)
Serve un host **HTTPS** (il service worker/offline lo richiede). Opzioni gratuite:
- **GitHub Pages**: pubblica il contenuto di `pwa/` → URL tipo `https://<utente>.github.io/<repo>/`.
- **Netlify Drop** (https://app.netlify.com/drop): trascina la cartella `pwa/`, ottieni un URL pubblico.

## Installare
- **iPhone (Safari)**: apri l'URL → tasto Condividi → **«Aggiungi a Home»**.
- **Android (Chrome)**: apri l'URL → menu ⋮ → **«Installa app»**.

## ⚠️ Backup
Su iPhone il browser può cancellare i dati se non usi l'app per un lungo periodo. Usa
**Impostazioni → Esporta dati (JSON)** ogni tanto per tenere un backup.
