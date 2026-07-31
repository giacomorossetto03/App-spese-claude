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

echo "==> Primo avvio (macOS può chiedere l'accesso al portachiavi: scegli \"Consenti sempre\")"
"$BIN_DIR/claude-usage.sh"
echo

installed_any=0

# --- Übersicht (widget sulla scrivania) -------------------------------------
# Il nome cartella contiene una umlaut: lo si cerca con una glob per evitare
# problemi di normalizzazione Unicode.
UB_DIR=""
for d in "$HOME/Library/Application Support/"*bersicht/widgets; do
  [ -d "$d" ] && UB_DIR="$d" && break
done

if [ -n "$UB_DIR" ]; then
  echo "==> Installo il widget Übersicht in: $UB_DIR"
  cp "$SRC/ubersicht/claude-usage.jsx" "$UB_DIR/claude-usage.jsx"
  installed_any=1
else
  echo "==> Übersicht non trovato — salto il widget da scrivania."
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

echo
if [ "$installed_any" = "1" ]; then
  echo "Fatto. Riavvia Übersicht (o SwiftBar) per vedere il widget."
else
  echo "Script installato, ma nessun host widget presente."
  echo "Installa Übersicht (scrivania) e/o SwiftBar (barra menu), poi rilancia."
fi
