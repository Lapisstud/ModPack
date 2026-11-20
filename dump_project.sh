#!/usr/bin/env bash
# dump_project.sh — собирает структуру и код проекта Minecraft-мода в один файл.
# Использование:
#   ./dump_project.sh                    # всё в project_dump_part_*.txt (по 70 КБ)
#   ./dump_project.sh my_dump.txt        # своё имя файла (будет my_dump_part_*.txt)
#   ./dump_project.sh out.txt java       # только структура + Java (без ресурсов)
#   ./dump_project.sh out.txt config     # только gradle + toml + json
#   ./dump_project.sh out.txt java src/main/java/com/example/grimoiremod/client # ТОЛЬКО папка client
#
# Переменные окружения:
#   DUMP_FULL_RESOURCES=1  не фильтровать ассет-мусор
#   DUMP_SHOW_BINARY=1     показывать в структуре бинарники
#   DUMP_MAX_BYTES=N       максимальный размер одного файла (default 200000)
#   DUMP_CHUNK_SIZE=70k    на куски какого размера бить дамп ("" чтобы отключить)
#   LANG_KEEP="en_us ru_ru" какой язык оставлять из lang/

set -euo pipefail

OUT="${1:-project_dump.txt}"
MODE="${2:-all}"
TARGET="${3:-src}"
MAX_BYTES="${DUMP_MAX_BYTES:-200000}"
LANG_KEEP="${LANG_KEEP:-en_us ru_ru}" # <--- ИСПРАВЛЕНО: теперь оставляем оба языка
CHUNK_SIZE="${DUMP_CHUNK_SIZE:-70k}"  # <--- ДОБАВЛЕНО: размер куска дампа

if [[ ! -f build.gradle && ! -f build.gradle.kts ]]; then
  echo "⚠  Не найден build.gradle(.kts). Запусти из корня проекта." >&2
  exit 1
fi

if [[ ! -d "$TARGET" ]]; then
  echo "⚠  Папка $TARGET не найдена. Проверь путь." >&2
  exit 1
fi

# --- Prune-список для структуры (целые директории) ---
PRUNE_DIRS=(
  .git .gradle build run runs out .idea .vscode .settings .eclipse bin
  logs .fabric .mixin.out remappedSrc .cache node_modules
)
FIND_PRUNE=()
for d in "${PRUNE_DIRS[@]}"; do
  FIND_PRUNE+=( -name "$d" -o )
done
unset 'FIND_PRUNE[${#FIND_PRUNE[@]}-1]'

# --- Бинарные расширения ---
BIN_EXTS=( png jpg jpeg gif ogg wav mp3 class jar zip nbt ttf otf )
FIND_BIN=()
if [[ -z "${DUMP_SHOW_BINARY:-}" ]]; then
  for e in "${BIN_EXTS[@]}"; do
    FIND_BIN+=( ! -name "*.$e" )
  done
fi

# --- Файлы, не попадающие ни в structure, ни в content ---
JUNK_NAMES=(
  GrimoirePreviewScreen.java
  WorkbenchPreviewScreen.java
  TestInterfacesScreen.java
  gradlew
  gradlew.bat
  project_dump*.txt
)
FIND_JUNK_NAMES=()
for n in "${JUNK_NAMES[@]}"; do
  FIND_JUNK_NAMES+=( ! -name "$n" )
done

# --- Мусорные ресурсы ---
RESOURCE_JUNK_GLOBS=(
  "*/models/*" "*/blockstates/*" "*/textures/*" "*/sounds/*" "*/fonts/*"
  "*/recipes/*" "*/loot_tables/*" "*/loottables/*"
  "*/tags/*" "*/advancements/*"
  "*/worldgen/*" "*/structures/*" "*/damage_type/*"
  "*/dimension/*" "*/dimension_type/*" "*/biome/*"
  "*/configured_feature/*" "*/placed_feature/*"
  "*/processor_list/*" "*/template_pool/*"
  "*/predicates/*" "*/item_modifiers/*"
  "*/trim_material/*" "*/trim_pattern/*"
)

is_resource_junk() {
  local f="${1#./}" p base keep_lang
  [[ -n "${DUMP_FULL_RESOURCES:-}" ]] && return 1
  for p in "${RESOURCE_JUNK_GLOBS[@]}"; do
    case "$f" in $p) return 0 ;; esac
  done
  # Проверка языков: если файл в lang/, проверяем, есть ли он в списке разрешенных
  if [[ -n "$LANG_KEEP" && "$f" == */lang/*.json ]]; then
    base=$(basename "$f" .json)
    keep_lang=0
    for lang in $LANG_KEEP; do
      if [[ "$base" == "$lang" ]]; then keep_lang=1; break; fi
    done
    [[ $keep_lang -eq 0 ]] && return 0
  fi
  return 1
}

# --- Skip-фильтр по basename (для content dump) ---
SKIP_BASENAMES=(
  GrimoirePreviewScreen.java
  WorkbenchPreviewScreen.java
  TestInterfacesScreen.java
  gradlew
  gradlew.bat
)
should_skip_content() {
  local base
  base=$(basename "$1")
  for n in "${SKIP_BASENAMES[@]}"; do
    [[ "$base" == "$n" ]] && return 0
  done
  case "$base" in
    *.sh|*.bak|*.bak.*|*.orig|*.rej|*~) return 0 ;;
  esac
  return 1
}

section() { printf '\n=================== %s ===================\n' "$1"; }
dump_file() {
  local f="$1" sz
  [[ -f "$f" ]] || return 0
  should_skip_content "$f" && return 0
  if ! grep -Iq . "$f" 2>/dev/null; then return 0; fi
  sz=$(wc -c < "$f" 2>/dev/null || echo 0)
  if (( sz > MAX_BYTES )); then
    printf '\n----- %s -----\n[пропущено: %s байт > %s]\n' "$f" "$sz" "$MAX_BYTES"
    return 0
  fi
  printf '\n----- %s -----\n' "$f"
  cat "$f" 2>/dev/null || true
}

OUT_BASE="$(basename "$OUT")"

# Создаем временный файл для записи всего дампа
TEMP_DUMP=$(mktemp)

{
  # --- Структура (фильтруется по TARGET, чтобы не засорять контекст) ---
  if [[ "$MODE" == "all" || "$MODE" == "java" || "$MODE" == "config" ]]; then
    section "PROJECT STRUCTURE"
    find . \
      \( -type d \( "${FIND_PRUNE[@]}" \) -prune \) -o \
      \( -type f \
         ! -name "$OUT_BASE" \
         ! -name '*.sh' \
         ! -name '*.orig' ! -name '*.rej' ! -name '*.bak*' ! -name '*~' \
         "${FIND_JUNK_NAMES[@]}" \
         "${FIND_BIN[@]}" \
         -print \) \
      | sort || true
  fi

  # --- Gradle / config ---
  if [[ "$MODE" == "all" || "$MODE" == "config" ]]; then
    section "GRADLE / CONFIG"
    for f in build.gradle build.gradle.kts settings.gradle settings.gradle.kts \
             gradle.properties gradle/wrapper/gradle-wrapper.properties; do
      dump_file "$f"
    done
  fi

  # --- Ресурсы (всегда из src/main/resources, если mode=config/all) ---
  if [[ "$MODE" == "all" || "$MODE" == "config" ]]; then
    if [[ -n "${DUMP_FULL_RESOURCES:-}" ]]; then
      section "RESOURCES (все toml/json/mcmeta/properties)"
    else
      section "RESOURCES (без ассет-мусора; lang keep=${LANG_KEEP:-all})"
    fi
    find src -type f \( -name "*.toml" -o -name "*.json" -o -name "*.mcmeta" -o -name "*.properties" \) 2>/dev/null \
      | sort | while read -r f; do
          is_resource_junk "$f" && continue
          dump_file "$f"
        done || true
  fi

  # --- Java (строго по указанному TARGET) ---
  if [[ "$MODE" == "all" || "$MODE" == "java" ]]; then
    section "JAVA SOURCES ($TARGET)"
    find "$TARGET" -type f -name "*.java" 2>/dev/null \
      | sort | while read -r f; do dump_file "$f"; done || true
  fi
} > "$TEMP_DUMP"

# Разбиваем или перемещаем файл
if [[ -n "$CHUNK_SIZE" ]] && command -v split >/dev/null 2>&1; then
  # Если в имени есть .txt, отрежем его для базы (например project_dump)
  BASE_NAME="${OUT%.txt}"
  split -C "$CHUNK_SIZE" -d -a 2 --additional-suffix=".txt" "$TEMP_DUMP" "${BASE_NAME}_part_"
  rm -f "$TEMP_DUMP"
  
  echo "✓ Готово! Дамп разбит на части (по $CHUNK_SIZE):"
  ls -1 "${BASE_NAME}"_part_*.txt | while read -r f; do
    SIZE=$(du -h "$f" | cut -f1)
    echo "   - $f ($SIZE)"
  done
else
  # Если split нет или CHUNK_SIZE пустой — делаем как раньше
  mv "$TEMP_DUMP" "$OUT"
  LINES=$(wc -l < "$OUT")
  SIZE=$(du -h "$OUT" | cut -f1)
  echo "✓ Готово: $OUT  ($LINES строк, $SIZE)"
fi