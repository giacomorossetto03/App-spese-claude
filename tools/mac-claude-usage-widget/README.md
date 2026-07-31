# Widget macOS — utilizzo del piano Claude

Widget per il Mac che tiene sott'occhio i limiti di utilizzo del piano Claude:
finestra a 5 ore, limite settimanale, quote per modello e reset.

> Nota: questa cartella **non fa parte dell'app Android** "Spese". È uno strumento
> di supporto, tenuto qui sotto `tools/` per comodità.

## Cosa mostra

- **Finestra 5 ore** — % consumata e quanto manca al reset
- **Settimana** — % consumata e quanto manca al reset
- **Opus / Sonnet (7 giorni)** — quote per modello, se il piano le espone
- **Extra usage** — solo se attivo

Colori: verde < 50%, giallo ≥ 50%, arancio ≥ 75%, rosso ≥ 90%.

## Da dove arrivano i dati

Da `GET https://api.anthropic.com/api/oauth/usage`, lo stesso endpoint che
alimenta il comando `/usage` di Claude Code. Le percentuali sono quindi quelle
ufficiali, non una stima ricavata dai log locali.

Il token OAuth viene letto, nell'ordine:

1. `$CLAUDE_CODE_OAUTH_TOKEN`
2. portachiavi macOS — `security find-generic-password -s "Claude Code-credentials" -w`
3. `~/.claude/.credentials.json`

Il token **non viene copiato né salvato** da nessuna parte: è letto in memoria a
ogni richiesta e usato solo verso `api.anthropic.com`.

## Installazione

Sul Mac, dalla cartella del repo:

```bash
cd tools/mac-claude-usage-widget
./install.sh
```

L'installer copia `claude-usage.sh` in `~/.local/bin`, lo esegue una volta e
installa il widget negli host che trova.

Serve almeno uno dei due host:

```bash
brew install --cask ubersicht   # widget sulla scrivania (consigliato)
brew install --cask swiftbar    # indicatore nella barra dei menu
```

Se non ne hai nessuno: installali, poi rilancia `./install.sh`.

**Al primo avvio** macOS chiede l'accesso al portachiavi: scegli **"Consenti
sempre"**, altrimenti il permesso verrà richiesto a ogni aggiornamento.
Übersicht e SwiftBar sono processi diversi, quindi ognuno chiederà il permesso
una volta.

## Posizione del widget sulla scrivania

Nel file `claude-usage.jsx`, dentro `className`:

```css
top: 40px;
right: 40px;
width: 260px;
```

Übersicht ricarica il widget al salvataggio, non serve riavviarlo.

## Frequenza di aggiornamento

L'endpoint `/api/oauth/usage` applica un rate limit severo, quindi lo script
tiene una cache in `~/.cache/claude-usage-widget/` e fa una richiesta reale al
massimo ogni 5 minuti. Il widget può aggiornarsi ogni minuto senza problemi:
tra una richiesta e l'altra rilegge la cache.

Per cambiare la finestra di cache:

```bash
CLAUDE_USAGE_TTL=600 ~/.local/bin/claude-usage.sh
```

Quando una richiesta fallisce (rate limit, rete, token scaduto) il widget
continua a mostrare l'ultimo dato valido con l'etichetta *"dato in cache"*.

## Uso da terminale

Lo script è utilizzabile anche da solo; stampa righe `chiave=valore`:

```
$ ~/.local/bin/claude-usage.sh
status=ok
five_hour_pct=78
five_hour_left=3h 13m
seven_day_pct=13
seven_day_left=4g 5h
opus_pct=
sonnet_pct=1
age=0
stale=0
```

In caso di problema stampa `status=error` e `error=<messaggio>`, sempre con
uscita 0, così il widget mostra il messaggio invece di una casella vuota.

## Se qualcosa non funziona

| Sintomo | Causa | Rimedio |
|---|---|---|
| `Nessun token Claude trovato` | mai fatto login, o portachiavi bloccato | lancia `claude` in un terminale e fai login |
| `Token scaduto` | access token OAuth scaduto | apri Claude Code: rinnova il token nel portachiavi, il widget lo riprende da solo |
| `Rate limit sull'endpoint usage` | troppe richieste | normale, si risolve da sé; eventualmente alza `CLAUDE_USAGE_TTL` |
| Widget vuoto in Übersicht | script non trovato | verifica che `~/.local/bin/claude-usage.sh` esista e sia eseguibile |
| Il widget non compare per niente | Übersicht non in esecuzione, o widget non caricato | aprilo dalle Applicazioni, poi menu nella barra → *Refresh All Widgets* |
| Il permesso portachiavi torna a ogni refresh | scelto "Consenti" invece di "Consenti sempre" | in *Accesso Portachiavi* → `Claude Code-credentials` → *Controllo accessi*, aggiungi l'app |

## File

```
bin/claude-usage.sh          raccolta dati + cache (bash, solo tool di sistema)
ubersicht/claude-usage.jsx   widget da scrivania (Übersicht)
swiftbar/claude-usage.1m.sh  plugin barra dei menu (SwiftBar / xbar)
install.sh                   installer
```
