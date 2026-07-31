#!/bin/bash
# claude-usage.sh — legge i limiti di utilizzo del piano Claude e li stampa
# come righe `chiave=valore`, pronte da consumare da un widget.
#
# Sorgente dati: GET https://api.anthropic.com/api/oauth/usage
# (lo stesso endpoint che alimenta il comando /usage di Claude Code).
#
# Dipendenze: solo tool di sistema macOS (curl, plutil, security, date).
#
# Variabili d'ambiente:
#   CLAUDE_USAGE_TTL   secondi di validità della cache (default 300)
#   CLAUDE_CODE_OAUTH_TOKEN  token da usare al posto del portachiavi

set -uo pipefail

ENDPOINT="https://api.anthropic.com/api/oauth/usage"
TTL="${CLAUDE_USAGE_TTL:-300}"
CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/claude-usage-widget"
CACHE_FILE="$CACHE_DIR/usage.json"

mkdir -p "$CACHE_DIR"

# --- helper -----------------------------------------------------------------

# Estrae un keypath da un file JSON. Stringa vuota se assente o null.
# Si legge da file (e non da stdin) perché è il caso supportato in modo
# uniforme da tutte le versioni di plutil.
json_get() {
  plutil -extract "$2" raw -o - "$1" 2>/dev/null
}

# Percentuale arrotondata all'intero. Vuoto se l'input non è un numero.
as_pct() {
  local v="$1"
  case "$v" in
    ''|*[!0-9.]*) printf '' ;;
    *) printf '%.0f' "$v" ;;
  esac
}

# Converte un timestamp ISO-8601 UTC in secondi mancanti da adesso.
secs_until() {
  local ts="${1:-}"
  [ ${#ts} -lt 19 ] && { printf ''; return; }
  local epoch
  epoch=$(TZ=UTC date -j -f "%Y-%m-%dT%H:%M:%S" "${ts:0:19}" "+%s" 2>/dev/null) || { printf ''; return; }
  printf '%s' "$(( epoch - $(date +%s) ))"
}

# Formatta una durata in secondi come "2h 14m" / "3g 4h" / "8m".
human_left() {
  local s="${1:-}"
  [ -z "$s" ] && { printf ''; return; }
  [ "$s" -le 0 ] && { printf 'ora'; return; }
  local d=$(( s / 86400 )) h=$(( (s % 86400) / 3600 )) m=$(( (s % 3600) / 60 ))
  if   [ "$d" -gt 0 ]; then printf '%dg %dh' "$d" "$h"
  elif [ "$h" -gt 0 ]; then printf '%dh %02dm' "$h" "$m"
  else                      printf '%dm' "$m"
  fi
}

fail() {
  printf 'status=error\nerror=%s\n' "$1"
  exit 0   # exit 0: il widget mostra il messaggio invece di una casella vuota
}

# --- token ------------------------------------------------------------------

read_token() {
  if [ -n "${CLAUDE_CODE_OAUTH_TOKEN:-}" ]; then
    printf '%s' "$CLAUDE_CODE_OAUTH_TOKEN"
    return 0
  fi

  local cred="$CACHE_DIR/cred.json" token=""
  if security find-generic-password -s "Claude Code-credentials" -w > "$cred" 2>/dev/null; then
    :
  elif [ -f "$HOME/.claude/.credentials.json" ]; then
    cp "$HOME/.claude/.credentials.json" "$cred" 2>/dev/null
  fi
  [ -s "$cred" ] || { rm -f "$cred"; return 1; }
  chmod 600 "$cred"

  token=$(json_get "$cred" "claudeAiOauth.accessToken")
  rm -f "$cred"
  [ -n "$token" ] || return 1
  printf '%s' "$token"
}

# --- fetch con cache --------------------------------------------------------

cache_age() {
  [ -f "$CACHE_FILE" ] || { printf '999999'; return; }
  printf '%s' "$(( $(date +%s) - $(stat -f %m "$CACHE_FILE") ))"
}

stale=0
fetch_err=""
age=$(cache_age)

if [ "$age" -ge "$TTL" ]; then
  token=$(read_token)
  if [ -z "${token:-}" ]; then
    fail "Nessun token Claude trovato. Esegui \`claude\` e fai login."
  fi

  tmp=$(mktemp "$CACHE_DIR/fetch.XXXXXX")
  code=$(curl -sS -m 15 -o "$tmp" -w '%{http_code}' \
      -H "Authorization: Bearer $token" \
      -H "anthropic-beta: oauth-2025-04-20" \
      -H "User-Agent: claude-usage-widget/1.0" \
      -H "Content-Type: application/json" \
      "$ENDPOINT" 2>/dev/null)

  case "$code" in
    200)
      mv "$tmp" "$CACHE_FILE"
      age=0
      ;;
    401|403)
      rm -f "$tmp"
      fetch_err="Token scaduto: apri Claude Code per rinnovarlo."
      ;;
    429)
      rm -f "$tmp"
      # L'endpoint applica un rate limit severo: si tiene il dato in cache.
      fetch_err="Rate limit sull'endpoint usage."
      ;;
    *)
      rm -f "$tmp"
      fetch_err="Richiesta fallita (HTTP ${code:-?})."
      ;;
  esac

  [ -n "$fetch_err" ] && stale=1
fi

if [ ! -f "$CACHE_FILE" ]; then
  fail "${fetch_err:-Nessun dato disponibile.}"
fi

# --- output -----------------------------------------------------------------

get() { json_get "$CACHE_FILE" "$1"; }

fh_pct=$(as_pct "$(get five_hour.utilization)")
fh_reset=$(get five_hour.resets_at)
sd_pct=$(as_pct "$(get seven_day.utilization)")
sd_reset=$(get seven_day.resets_at)
op_pct=$(as_pct "$(get seven_day_opus.utilization)")
so_pct=$(as_pct "$(get seven_day_sonnet.utilization)")
xu_on=$(get extra_usage.is_enabled)
xu_pct=$(as_pct "$(get extra_usage.utilization)")

printf 'status=ok\n'
printf 'five_hour_pct=%s\n'    "$fh_pct"
printf 'five_hour_left=%s\n'   "$(human_left "$(secs_until "$fh_reset")")"
printf 'seven_day_pct=%s\n'    "$sd_pct"
printf 'seven_day_left=%s\n'   "$(human_left "$(secs_until "$sd_reset")")"
printf 'opus_pct=%s\n'         "$op_pct"
printf 'sonnet_pct=%s\n'       "$so_pct"
# plutil rende i booleani come "true"/"false" o "1"/"0" a seconda della versione.
case "$xu_on" in true|1) [ -n "$xu_pct" ] && printf 'extra_pct=%s\n' "$xu_pct" ;; esac
printf 'age=%s\n'              "$age"
printf 'stale=%s\n'            "$stale"
[ -n "$fetch_err" ] && printf 'note=%s\n' "$fetch_err"

exit 0
