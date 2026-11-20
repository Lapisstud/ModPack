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
