#!/usr/bin/env bash
# Одноразовый скрипт настройки git-репозитория. Запускать из корня проекта.
set -euo pipefail

if [ ! -d ".git" ]; then
    echo "git не инициализирован. Сначала: git init && git lfs install"
    exit 1
fi

echo "==> Настраиваю git LFS..."
git lfs track "*.ogg" "*.wav" "*.mp3" "*.png" "*.bbmodel" "*.nbt" "*.litematic" "*.jar" >/dev/null

echo "==> Дополняю .gitignore..."
GITIGNORE_ADD='
# === Grimoire modpack (auto-added by tools/setup_git.sh) ===
modpack/resourcepacks/grimoire_assets/assets/grimoiremod/sounds.json
modpack/resourcepacks/grimoire_assets/assets/grimoiremod/sounds/**/*.ogg
modpack/resourcepacks/grimoire_assets/assets/grimoiremod/textures/**/*.png
modpack/resourcepacks/grimoire_assets/assets/grimoiremod/models/**/*.json
modpack/resourcepacks/grimoire_assets/data/grimoiremod/structures/**/*.nbt

modpack/mods/*.jar
!modpack/mods/.gitkeep

modpack/manifest.json

launcher/build/
launcher/dist/
launcher/node_modules/
site/.cache/
site/public/

*.log
.patch_backup_*/
run/
'

if ! grep -q "auto-added by tools/setup_git.sh" .gitignore 2>/dev/null; then
    echo "$GITIGNORE_ADD" >> .gitignore
    echo "  .gitignore дополнен."
else
    echo "  .gitignore уже содержит наши правила, пропускаю."
fi

echo "==> Создаю .gitkeep в пустых папках..."
for d in modpack/mods modpack/config modpack/resourcepacks/grimoire_assets launcher site; do
    mkdir -p "$d"
    [ -f "$d/.gitkeep" ] || touch "$d/.gitkeep"
done

echo "==> Готово. Дальше: git add -A && git commit"
