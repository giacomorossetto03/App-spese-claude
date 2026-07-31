#!/bin/bash
# Installa il widget "utilizzo Claude" sul Mac.
# Da eseguire sul Mac, dentro questa cartella:  ./install.sh

set -uo pipefail

SRC="$(cd "$(dirname "$0")" && pwd)"
BIN_DIR="$HOME/.local/bin"

if [ "$(uname -s)" != "Darwin" ]; then
  echo "Questo installer è per macOS." >&2
  exit 1
fi

echo "==> Installo lo script di raccolta dati in $BIN_DIR"
mkdir -p "$BIN_DIR"
cp "$SRC/bin/claude-usage.sh" "$BIN_DIR/claude-usage.sh"
chmod +x "$BIN_DIR/claude-usage.sh"
echo "    versione: $("$BIN_DIR/claude-usage.sh" --version)"

# Un `git pull` aggiorna il repo, non questa copia: senza rilanciare l'installer
# si continuerebbe a eseguire la versione vecchia.

echo "==> Primo avvio (macOS può chiedere l'accesso al portachiavi: scegli \"Consenti sempre\")"
"$BIN_DIR/claude-usage.sh"
echo

installed_any=0

# --- Übersicht (widget sulla scrivania) -------------------------------------
# Il nome cartella contiene una umlaut: lo si cerca con una glob per evitare
# problemi di normalizzazione Unicode.
UB_DIR=""

# La cartella widgets nasce solo al primo avvio di Übersicht: non ci si può
# basare per capire se l'app c'è. Si cerca l'app, e la cartella la si crea.
UB_APP=""
for a in "/Applications/"*bersicht.app "$HOME/Applications/"*bersicht.app; do
  [ -d "$a" ] && UB_APP="$a" && break
done

# Se l'app è già stata avviata almeno una volta si riusa la cartella esistente,
# così si rispetta un eventuale percorso personalizzato.
for d in "$HOME/Library/Application Support/"*bersicht/widgets; do
  [ -d "$d" ] && UB_DIR="$d" && break
done

if [ -n "$UB_APP" ]; then
  if [ -z "$UB_DIR" ]; then
    UB_DIR="$HOME/Library/Application Support/Übersicht/widgets"
    mkdir -p "$UB_DIR"
  fi
  echo "==> Installo il widget Übersicht in: $UB_DIR"
  cp "$SRC/ubersicht/claude-usage.jsx" "$UB_DIR/claude-usage.jsx"
  installed_any=1

  echo "==> Avvio Übersicht e ricarico i widget"
  open -a "$UB_APP" 2>/dev/null
  sleep 2
  # Forza il rescan della cartella widget senza dover riavviare a mano.
  open -g "ubersicht://refresh" 2>/dev/null
else
  echo "==> Übersicht non trovato in /Applications — salto il widget da scrivania."
  echo "    Installalo con:  brew install --cask ubersicht"
  echo "    poi rilancia questo script."
fi

# --- SwiftBar / xbar (barra dei menu) ---------------------------------------
SB_DIR=""
for d in "$HOME/.swiftbar" "$HOME/Library/Application Support/SwiftBar/Plugins" \
         "$HOME/Library/Application Support/xbar/plugins"; do
  [ -d "$d" ] && SB_DIR="$d" && break
done

if [ -n "$SB_DIR" ]; then
  echo "==> Installo il plugin SwiftBar/xbar in: $SB_DIR"
  cp "$SRC/swiftbar/claude-usage.1m.sh" "$SB_DIR/claude-usage.1m.sh"
  chmod +x "$SB_DIR/claude-usage.1m.sh"
  installed_any=1
else
  echo "==> SwiftBar/xbar non trovato — salto il plugin da barra dei menu."
  echo "    Opzionale:  brew install --cask swiftbar"
fi

# --- verifica finale --------------------------------------------------------

echo
echo "==> Verifica"

out=$("$BIN_DIR/claude-usage.sh" 2>&1)
status=$(printf '%s\n' "$out" | grep '^status=' | cut -d= -f2-)
if [ "$status" = "ok" ]; then
  fh=$(printf '%s\n' "$out" | grep '^five_hour_pct=' | cut -d= -f2-)
  sd=$(printf '%s\n' "$out" | grep '^seven_day_pct=' | cut -d= -f2-)
  echo "    [ok] dati letti — 5 ore: ${fh}%, settimana: ${sd}%"
else
  echo "    [KO] lo script non legge i dati:"
  printf '%s\n' "$out" | grep '^error=' | sed 's/^error=/         /'
  echo "         Il widget mostrerà questo messaggio finché non si risolve."
fi

if [ -n "$UB_DIR" ] && [ -f "$UB_DIR/claude-usage.jsx" ]; then
  echo "    [ok] widget copiato in $UB_DIR"
else
  echo "    [--] widget da scrivania non installato"
fi

# Si controlla il processo reale dell'app, non un `open` di passaggio.
if pgrep -f "bersicht.app/Contents/MacOS" > /dev/null 2>&1; then
  echo "    [ok] Übersicht è in esecuzione"
  echo "         (è un'app da barra dei menu: nessuna finestra, nessuna icona nel Dock)"
elif [ -n "$UB_APP" ]; then
  echo "    [KO] Übersicht non risulta in esecuzione"
  echo "         Aprilo dalle Applicazioni. Se macOS blocca l'avvio:"
  echo "         Impostazioni di Sistema > Privacy e Sicurezza > \"Apri comunque\""
fi

echo
if [ "$installed_any" = "1" ]; then
  echo "Fatto. Il widget compare in alto a destra sulla scrivania."
  echo "Se non lo vedi: menu di Übersicht nella barra > Refresh All Widgets."
else
  echo "Script installato, ma nessun host widget presente."
  echo "Installa Übersicht (scrivania) e/o SwiftBar (barra menu), poi rilancia."
fi
