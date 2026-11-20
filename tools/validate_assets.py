#!/usr/bin/env python3
"""Валидатор ассетов. Запускается ПЕРЕД коммитом. 0 = ок, 1 = ошибки."""
import json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "assets-src"
NAME_RE = re.compile(r"^[a-z0-9._-]+$")
KNOWN_EXT = {".mp3", ".wav", ".ogg", ".png", ".bbmodel", ".json", ".nbt", ".litematic"}


def scan():
    errors, warnings = [], []
    if not ASSETS.exists():
        print(f"WARN: {ASSETS} не существует. Нечего проверять.")
        return errors, warnings
    for p in ASSETS.rglob("*"):
        if p.is_dir():
            if p != ASSETS and not NAME_RE.match(p.name):
                errors.append(f"[имя папки] {p.relative_to(ROOT)} — только [a-z0-9._-]")
            continue
        rel = p.relative_to(ROOT)
        if not NAME_RE.match(p.stem):
            errors.append(f"[имя файла] {rel} — только [a-z0-9._-]")
            continue
        ext = p.suffix.lower()
        if ext not in KNOWN_EXT:
            warnings.append(f"[формат] {rel} — {ext} не в пайплайне")
            continue
        if ext == ".png":
            with open(p, "rb") as f:
                if f.read(8) != b"\x89PNG\r\n\x1a\n":
                    errors.append(f"[png] {rel} — повреждён")
        elif ext == ".json":
            try:
                json.loads(p.read_text(encoding="utf-8"))
            except Exception as e:
                errors.append(f"[json] {rel} — {e}")
        elif ext in {".nbt", ".litematic"} and p.stat().st_size == 0:
            errors.append(f"[структура] {rel} — пустой файл")
    return errors, warnings


def main():
    errors, warnings = scan()
    for w in warnings: print(f"WARN: {w}")
    for e in errors: print(f"FAIL: {e}")
    if errors:
        print(f"\n{len(errors)} ошибок.")
        sys.exit(1)
    print("OK: ассеты валидны.")
    sys.exit(0)


if __name__ == "__main__":
    main()
