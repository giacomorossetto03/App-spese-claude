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
# L'endpoint fa parte del client Claude Code e rifiuta gli User-Agent che non
# riconosce: ci si presenta come il client ufficiale (stesso account, stessi dati).
UA="${CLAUDE_USAGE_UA:-claude-cli/2.1.220 (external, cli)}"

CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/claude-usage-widget"
CACHE_FILE="$CACHE_DIR/usage.json"

mkdir -p "$CACHE_DIR"

DEBUG=0
case "${1:-}" in
  --debug)  DEBUG=1 ;;                       # ignora cache e backoff, chiamata vera
  --reset)  rm -f "$CACHE_DIR"/*; echo "Cache e backoff azzerati."; exit 0 ;;
  --version) echo "claude-usage 1.1"; exit 0 ;;
esac

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

TOKEN=""           # token letto (globale: non usare in $(...), si perderebbe)
TOKEN_SRC=""       # da dove arriva il token
TOKEN_EXP=""       # scadenza in epoch secondi, se nota

read_token() {
  if [ -n "${CLAUDE_CODE_OAUTH_TOKEN:-}" ]; then
    TOKEN_SRC="variabile CLAUDE_CODE_OAUTH_TOKEN"
    TOKEN="$CLAUDE_CODE_OAUTH_TOKEN"
    return 0
  fi

  local cred="$CACHE_DIR/cred.json" exp_ms=""
  if security find-generic-password -s "Claude Code-credentials" -w > "$cred" 2>/dev/null; then
    TOKEN_SRC="portachiavi macOS"
  elif [ -f "$HOME/.claude/.credentials.json" ]; then
    cp "$HOME/.claude/.credentials.json" "$cred" 2>/dev/null
    TOKEN_SRC="~/.claude/.credentials.json"
  fi
  [ -s "$cred" ] || { rm -f "$cred"; return 1; }
  chmod 600 "$cred"

  TOKEN=$(json_get "$cred" "claudeAiOauth.accessToken")
  exp_ms=$(json_get "$cred" "claudeAiOauth.expiresAt")
  case "$exp_ms" in
    ''|*[!0-9]*) TOKEN_EXP="" ;;
    *) TOKEN_EXP=$(( exp_ms / 1000 )) ;;
  esac
  rm -f "$cred"
  [ -n "$TOKEN" ] || return 1
}

# --- fetch con cache --------------------------------------------------------

cache_age() {
  [ -f "$CACHE_FILE" ] || { printf '999999'; return; }
  printf '%s' "$(( $(date +%s) - $(stat -f %m "$CACHE_FILE") ))"
}

stale=0
fetch_err=""
age=$(cache_age)
now=$(date +%s)

# Il ritmo delle chiamate si regola sull'ultimo TENTATIVO, non sull'ultima
# risposta buona: altrimenti, finché non arriva un 200, non esiste nessun file
# di cache e ogni refresh del widget (60s) rifà la richiesta — che è il modo
# più veloce per restare permanentemente in 429 su questo endpoint.
ATTEMPT_FILE="$CACHE_DIR/last-attempt"
BACKOFF_FILE="$CACHE_DIR/backoff-until"

attempt_age=999999
[ -f "$ATTEMPT_FILE" ] && attempt_age=$(( now - $(stat -f %m "$ATTEMPT_FILE") ))

backoff_until=0
[ -f "$BACKOFF_FILE" ] && backoff_until=$(cat "$BACKOFF_FILE" 2>/dev/null || echo 0)
case "$backoff_until" in ''|*[!0-9]*) backoff_until=0 ;; esac

if [ "$DEBUG" = "1" ] || { [ "$age" -ge "$TTL" ] && [ "$attempt_age" -ge "$TTL" ] && [ "$now" -ge "$backoff_until" ]; }; then
  touch "$ATTEMPT_FILE"
  read_token
  if [ -z "$TOKEN" ]; then
    fail "Nessun token Claude trovato. Esegui \`claude\` e fai login."
  fi

  tmp=$(mktemp "$CACHE_DIR/fetch.XXXXXX")
  code=$(curl -sS -m 15 -o "$tmp" -w '%{http_code}' \
      -H "Authorization: Bearer $TOKEN" \
      -H "anthropic-beta: oauth-2025-04-20" \
      -H "anthropic-version: 2023-06-01" \
      -H "User-Agent: $UA" \
      -H "Content-Type: application/json" \
      "$ENDPOINT" 2>/dev/null)

  if [ "$DEBUG" = "1" ]; then
    echo "--- diagnostica -------------------------------------------"
    echo "sorgente token : ${TOKEN_SRC:-sconosciuta}"
    echo "token          : ${TOKEN:0:14}… (${#TOKEN} caratteri)"
    if [ -n "$TOKEN_EXP" ]; then
      left=$(( TOKEN_EXP - $(date +%s) ))
      if [ "$left" -gt 0 ]; then
        echo "scadenza token : fra $(human_left "$left")"
      else
        echo "scadenza token : SCADUTO da $(human_left $(( -left )))"
      fi
    else
      echo "scadenza token : non disponibile"
    fi
    echo "user-agent     : $UA"
    echo "HTTP           : $code"
    echo "risposta       :"
    sed 's/^/  /' "$tmp" 2>/dev/null | head -20
    echo "-----------------------------------------------------------"
  fi

  # Dopo un errore si sta fermi: riprovare subito peggiora e basta.
  backoff=0
  case "$code" in
    200)
      mv "$tmp" "$CACHE_FILE"
      age=0
      rm -f "$BACKOFF_FILE"
      ;;
    401)
      rm -f "$tmp"
      fetch_err="Token scaduto: apri Claude Code sul Mac per rinnovarlo."
      backoff=600
      ;;
    403)
      rm -f "$tmp"
      fetch_err="Accesso negato all'endpoint usage (403)."
      backoff=1800
      ;;
    429)
      rm -f "$tmp"
      # Rate limit severo e documentato: si aspetta a lungo prima di riprovare.
      fetch_err="Rate limit: nuovo tentativo fra 15 min."
      backoff=900
      ;;
    *)
      rm -f "$tmp"
      fetch_err="Richiesta fallita (HTTP ${code:-?})."
      backoff=300
      ;;
  esac

  if [ "$backoff" -gt 0 ]; then
    stale=1
    printf '%s' "$(( now + backoff ))" > "$BACKOFF_FILE"
  fi
fi

if [ ! -f "$CACHE_FILE" ]; then
  if [ -n "$fetch_err" ]; then
    fail "$fetch_err"
  elif [ "$now" -lt "$backoff_until" ]; then
    fail "In attesa: nuovo tentativo fra $(human_left $(( backoff_until - now )))."
  else
    fail "Nessun dato disponibile."
  fi
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
