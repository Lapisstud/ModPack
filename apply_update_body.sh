mkdir -p site launcher tools

# ==========================================
# 1. САЙТ (GitHub Pages)
# ==========================================
cat > 'site/index.html' <<'EOF_SITE'
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Grimoire Modpack</title>
    <style>
        body {
            background-color: #1a1a2e;
            color: #fff;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            margin: 0;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            height: 100vh;
        }
        .container {
            text-align: center;
            background: #16213e;
            padding: 40px;
            border-radius: 15px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.5);
            max-width: 500px;
        }
        h1 { color: #e94560; margin-bottom: 10px; }
        p { color: #a0a0a0; margin-bottom: 30px; }
        .btn {
            display: block;
            width: 100%;
            padding: 15px;
            margin: 10px 0;
            border: none;
            border-radius: 8px;
            font-size: 18px;
            font-weight: bold;
            cursor: pointer;
            text-decoration: none;
            transition: 0.3s;
        }
        .btn-primary {
            background: #e94560;
            color: white;
        }
        .btn-primary:hover { background: #ff5775; }
        .btn-secondary {
            background: transparent;
            color: #e94560;
            border: 2px solid #e94560;
        }
        .btn-secondary:hover { background: #e94560; color: white; }
    </style>
</head>
<body>
    <div class="container">
        <h1>Grimoire Modpack</h1>
        <p>Создавай свои заклинания, исследуй данжи и развивай мастерство.</p>
        <!-- Замени ссылку ниже на свой релиз на GitHub, когда он появится -->
        <a href="https://github.com/Lapisstud/ModPack/releases/latest/download/GrimoireLauncher.exe" class="btn btn-primary">Скачать Лаунчер</a>
        <a href="https://github.com/Lapisstud/ModPack" class="btn btn-secondary" target="_blank">GitHub Репозиторий</a>
    </div>
</body>
</html>
EOF_SITE


# ==========================================
# 2. ГЕНЕРАТОР МАНИФЕСТА
# ==========================================
cat > 'tools/build_manifest.py' <<'EOF_MANIFEST'
#!/usr/bin/env python3
"""
Генератор manifest.json для лаунчера.
Сканирует папку modpack/, считает SHA256 каждого файла.
"""
import hashlib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MODPACK_DIR = ROOT / "modpack"
OUT_FILE = MODPACK_DIR / "manifest.json"

# Замени на свой репозиторий (для скачивания напрямую с GitHub Releases)
REPO_URL = "https://raw.githubusercontent.com/Lapisstud/ModPack/test/modpack/"

def get_sha256(filepath):
    sha256_hash = hashlib.sha256()
    with open(filepath, "rb") as f:
        for byte_block in iter(lambda: f.read(4096), b""):
            sha256_hash.update(byte_block)
    return sha256_hash.hexdigest()

def main():
    if not MODPACK_DIR.exists():
        print("FAIL: Папка modpack/ не найдена.")
        sys.exit(1)

    files = []
    for f in MODPACK_DIR.rglob("*"):
        if f.is_dir() or f.name == "manifest.json" or f.name == ".gitkeep":
            continue
        
        rel_path = f.relative_to(MODPACK_DIR).as_posix()
        sha = get_sha256(f)
        
        # В будущем url можно заменить на прямые ссылки релизов
        url = f"{REPO_URL}{rel_path}"
        
        files.append({
            "path": rel_path,
            "sha256": sha,
            "size": f.stat().st_size,
            "url": url
        })
        print(f"  + {rel_path} ({f.stat().st_size} bytes)")

    manifest = {
        "version": "1.0.0",  # В будущем можно сделать автоматом
        "minecraft": "1.21.1",
        "neoforge": "21.1.267",
        "files": files
    }

    OUT_FILE.write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print(f"OK: Манифест создан -> {OUT_FILE.relative_to(ROOT)} ({len(files)} файлов)")

if __name__ == "__main__":
    main()
EOF_MANIFEST
chmod +x tools/build_manifest.py

# ==========================================
# 3. АРХИТЕКТУРА ЛАУНЧЕРА
# ==========================================
cat > 'launcher/requirements.txt' <<'EOF_REQS'
customtkinter==5.2.2
requests==2.31.0
EOF_REQS

cat > 'launcher/main.py' <<'EOF_LAUNCHER'
import os
import sys
import json
import hashlib
import threading
import requests
import customtkinter as ctk
from pathlib import Path
from tkinter import messagebox

# --- Конфигурация ---
MANIFEST_URL = "https://raw.githubusercontent.com/Lapisstud/ModPack/test/modpack/manifest.json"
MINECRAFT_DIR = Path(os.getenv('APPDATA')) / '.grimoire_modpack' if os.name == 'nt' else Path.home() / '.grimoire_modpack'
MODS_DIR = MINECRAFT_DIR / "mods"

ctk.set_appearance_mode("dark")
ctk.set_default_color_theme("dark-blue")

class LauncherApp(ctk.CTk):
    def __init__(self):
        super().__init__()
        self.title("Grimoire Launcher")
        self.geometry("500x350")
        self.resizable(False, False)

        self.label = ctk.CTkLabel(self, text="Grimoire Modpack", font=ctk.CTkFont(size=24, weight="bold"))
        self.label.pack(pady=(30, 10))

        self.status = ctk.CTkLabel(self, text="Нажмите 'Играть' для проверки обновлений", text_color="gray")
        self.status.pack(pady=10)

        self.progress = ctk.CTkProgressBar(self, width=400)
        self.progress.pack(pady=10)
        self.progress.set(0)

        self.play_btn = ctk.CTkButton(self, text="Играть", command=self.start_game, height=50, font=ctk.CTkFont(size=18))
        self.play_btn.pack(pady=20)

    def log(self, text):
        self.status.configure(text=text)
        self.update_idletasks()

    def start_game(self):
        self.play_btn.configure(state="disabled")
        threading.Thread(target=self.update_and_launch, daemon=True).start()

    def update_and_launch(self):
        try:
            self.log("Скачивание манифеста...")
            resp = requests.get(MANIFEST_URL, timeout=10)
            resp.raise_for_status()
            manifest = resp.json()

            files = manifest.get("files", [])
            self.progress.set(0)
            MODS_DIR.mkdir(parents=True, exist_ok=True)

            for i, file_info in enumerate(files):
                path = file_info["path"]
                if not path.startswith("mods/"):
                    continue  # Пока качаем только моды

                filename = Path(path).name
                local_file = MODS_DIR / filename
                expected_hash = file_info["sha256"]

                self.log(f"Проверка: {filename}")
                
                # Проверяем хэш, если файл есть
                if local_file.exists():
                    local_hash = self.get_sha256(local_file)
                    if local_hash == expected_hash:
                        continue

                # Скачиваем
                self.log(f"Скачивание: {filename}")
                r = requests.get(file_info["url"], stream=True)
                with open(local_file, 'wb') as f:
                    for chunk in r.iter_content(chunk_size=8192):
                        f.write(chunk)

                self.progress.set((i + 1) / len(files))

            self.log("Обновление завершено!")
            self.progress.set(1)
            
            # Запуск игры (заглушка, тут будет вызов subprocess)
            messagebox.showinfo("Готово", "Модпак обновлен!\n(Запуск игры пока не реализован)")
            self.play_btn.configure(state="normal")

        except Exception as e:
            self.log(f"Ошибка: {str(e)}")
            self.play_btn.configure(state="normal")

    @staticmethod
    def get_sha256(filepath):
        sha256 = hashlib.sha256()
        with open(filepath, "rb") as f:
            for chunk in iter(lambda: f.read(4096), b""):
                sha256.update(chunk)
        return sha256.hexdigest()

if __name__ == "__main__":
    app = LauncherApp()
    app.mainloop()
EOF_LAUNCHER

echo "============================================"
echo "OK: Сайт, манифест и лаунчер созданы."