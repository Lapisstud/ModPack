# 1. Дополняем .gitignore
cat >> .gitignore <<'EOF_JUNK'

# === IDE / build artifacts (added by hand, do not remove) ===
bin/
.settings/
.classpath
.project
.idea/
*.iml
*.launch
.vscode/

# === Temp / dump files ===
d.txt
project_dump*.txt
project_dump_part_*.txt
*.bak.*
*.orig
*.rej
EOF_JUNK

# 2. Создаём .gitattributes — нормализация переводов строк
cat > .gitattributes <<'EOF_ATTR'
# Normalize line endings: LF in repo, native on checkout
* text=auto eol=lf

# Shell scripts — строго LF (иначе bash ругается на Windows)
*.sh   text eol=lf
*.bash text eol=lf

# Windows batch — CRLF
*.bat  text eol=crlf
*.cmd  text eol=crlf

# Binary files — не трогать
*.jar  binary
*.png  binary
*.ogg  binary
*.mp3  binary
*.wav  binary
*.bbmodel binary
*.nbt  binary
*.litematic binary
EOF_ATTR

# 3. Сбрасываем index — чтобы .gitignore переприменился
git rm -r --cached . >/dev/null 2>&1 || true

# 4. Проверяем, что мусора больше нет
echo "=== Файлов в staging ==="
git add -A --dry-run 2>&1 | wc -l
echo "=== Проверка: bin, .eclipse, d.txt, dumps ==="
git add -A --dry-run 2>&1 | grep -E "bin/|\.eclipse|^add 'd\.txt|project_dump|\.bak\." || echo "OK: мусора нет"