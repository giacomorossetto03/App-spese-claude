#!/bin/bash
# <bitbar.title>Claude Usage</bitbar.title>
# <bitbar.version>v1.0</bitbar.version>
# <bitbar.desc>Limiti di utilizzo del piano Claude nella barra dei menu.</bitbar.desc>
# <bitbar.dependencies>bash</bitbar.dependencies>
#
# Plugin SwiftBar / xbar. Copiare in ~/.swiftbar (o nella cartella plugin di xbar)
# e renderlo eseguibile. Richiede ~/.local/bin/claude-usage.sh.

CORE="$HOME/.local/bin/claude-usage.sh"

if [ ! -x "$CORE" ]; then
  echo "Claude ⚠️"
  echo "---"
  echo "claude-usage.sh non trovato in ~/.local/bin"
  exit 0
fi

out=$("$CORE")

get() { printf '%s\n' "$out" | grep "^$1=" | cut -d= -f2-; }

status=$(get status)

if [ "$status" != "ok" ]; then
  echo "Claude ⚠️"
  echo "---"
  echo "$(get error)"
  exit 0
fi

fh=$(get five_hour_pct)
sd=$(get seven_day_pct)

dot() {
  local p="${1:-0}"
  if   [ "$p" -ge 90 ]; then printf '🔴'
  elif [ "$p" -ge 75 ]; then printf '🟠'
  elif [ "$p" -ge 50 ]; then printf '🟡'
  else                       printf '🟢'
  fi
}

# Titolo: il vincolo più stretto fra finestra 5 ore e settimana.
if [ "${fh:-0}" -ge "${sd:-0}" ]; then worst="$fh"; else worst="$sd"; fi
echo "$(dot "$worst") ${fh}% · ${sd}% | font=SF Mono size=12"

echo "---"
echo "Finestra 5 ore: ${fh}%  —  reset tra $(get five_hour_left) | font=SF Mono"
echo "Settimana: ${sd}%  —  reset tra $(get seven_day_left) | font=SF Mono"

op=$(get opus_pct); so=$(get sonnet_pct); xu=$(get extra_pct)
if [ -n "$op" ] || [ -n "$so" ] || [ -n "$xu" ]; then
  echo "---"
  [ -n "$op" ] && echo "Opus (7g): ${op}% | font=SF Mono"
  [ -n "$so" ] && echo "Sonnet (7g): ${so}% | font=SF Mono"
  [ -n "$xu" ] && echo "Extra usage: ${xu}% | font=SF Mono"
fi

echo "---"
[ "$(get stale)" = "1" ] && echo "⚠️ $(get note)"
echo "Aggiornato $(get age)s fa"
echo "Aggiorna ora | refresh=true"
