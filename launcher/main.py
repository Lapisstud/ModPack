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
