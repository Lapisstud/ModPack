#!/usr/bin/env bash
# apply_update.sh — ОБЁРТКА. Не редактируй этот файл.
# Код обновления кладётся в apply_update_body.sh (select all → paste).
#
# Флаги:
#   SKIP_COMPILE=1   не запускать gradlew compileJava
#   SKIP_DUMP=1      не запускать dump_project.sh
#   SKIP_RUN=1       не запускать gradlew runClient
#
# Код возврата:
#   0 — всё ок
#   1 — нет apply_update_body.sh
#   2 — тело обновления упало
#   3 — compileJava упал
#   4 — runClient упал

set -uo pipefail
cd "$(dirname "$0")"   # всегда работаем из корня проекта

BODY="${BODY:-apply_update_body.sh}"
DUMP="${DUMP:-./dump_project.sh}"
GRADLE="${GRADLE:-./gradlew}"

step() { printf '\n\033[1;36m▶ %s\033[0m\n' "$*"; }
warn() { printf '\033[1;33m⚠  %s\033[0m\n' "$*" >&2; }
err()  { printf '\033[1;31m✗  %s\033[0m\n' "$*" >&2; }

# --- 0. Проверки ---
[[ -f "$BODY" ]] || { err "нет $BODY"; exit 1; }
[[ -x "$GRADLE" ]] || { warn "нет $GRADLE — компиляция/запуск пропущены"; GRADLE=""; }

# CRLF-фикс на случай copy-paste из Windows
if grep -q $'\r' "$BODY" 2>/dev/null; then
  warn "в $BODY найдены CRLF — чиню"
  sed -i 's/\r$//' "$BODY"
fi

EXIT=0

# --- 1. Тело обновления ---
step "apply_update_body.sh"
if ! bash "$BODY"; then
  err "тело обновления упало (идём дальше — дамп нужен для диагностики)"
  EXIT=2
fi

# --- 2. compileJava ---
COMPILE_OK=0
if [[ -n "$GRADLE" && -z "${SKIP_COMPILE:-}" ]]; then
  step "gradlew compileJava"
  if "$GRADLE" compileJava; then
    COMPILE_OK=1
  else
    err "compileJava упал"
    EXIT=3
  fi
fi

# --- 3. Дамп (всегда, даже если компиляция упала) ---
if [[ -z "${SKIP_DUMP:-}" ]]; then
  if [[ -x "$DUMP" ]]; then
    step "dump_project.sh"
    "$DUMP" || warn "дамп упал (не критично)"
  else
    warn "dump_project.sh не найден — пропускаю"
  fi
fi

# --- 4. runClient (только если скомпилировалось) ---
if [[ -n "$GRADLE" && -z "${SKIP_RUN:-}" ]]; then
  if (( COMPILE_OK )); then
    step "gradlew runClient"
    "$GRADLE" runClient || { err "runClient упал"; EXIT=4; }
  else
    warn "пропускаю runClient (compileJava не прошёл)"
  fi
fi

step "готово (exit $EXIT)"
exit $EXIT