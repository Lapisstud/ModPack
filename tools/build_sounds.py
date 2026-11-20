#!/usr/bin/env python3
"""Конвертация звуков + генерация sounds.json."""
import json, re, shutil, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "assets-src" / "sounds"
DST_ROOT = ROOT / "modpack" / "resourcepacks" / "grimoire_assets" / "assets" / "grimoiremod"
DST_SOUNDS = DST_ROOT / "sounds"
JSON_OUT = DST_ROOT / "sounds.json"

TRIGGERS = {
    "book.use": "player", "book.open": "player", "book.close": "player",
    "book.page": "player", "book.error": "player",
    "glyph.select": "player", "glyph.place": "player",
    "glyph.remove": "player", "glyph.point": "player",
    "workbench.open": "player", "workbench.close": "player",
    "skilltree.open": "player", "skilltree.unlock": "player",
    "spell.cast": "player", "spell.cast.projectile": "player",
    "spell.cast.self": "player", "spell.cast.touch": "player",
    "spell.hit.fire": "player", "spell.hit.ice": "player",
    "spell.hit.water": "player", "spell.hit.heal": "player",
    "spell.impact.block": "player", "ui.click": "master",
}
NAME_RE = re.compile(r"^[a-z0-9._-]+$")


def die(msg):
    print(f"FAIL: {msg}", file=sys.stderr)
    sys.exit(1)


def parse_name(stem):
    parts = stem.split(".")
    if len(parts) >= 2 and parts[-1].isdigit() and len(parts[-1]) == 1:
        return ".".join(parts[:-1]), parts[-1]
    return stem, None


def convert(src, dst):
    dst.parent.mkdir(parents=True, exist_ok=True)
    if src.suffix.lower() == ".ogg":
        shutil.copy(src, dst)
        return
    r = subprocess.run(
        ["ffmpeg", "-y", "-i", str(src), "-c:a", "libvorbis", "-q:a", "5",
         "-ar", "44100", "-ac", "1", str(dst)],
        capture_output=True)
    if r.returncode != 0:
        die(f"ffmpeg упал на {src.name}:\n{r.stderr.decode(errors='replace')}")


def main():
    if not shutil.which("ffmpeg"):
        die("ffmpeg не найден. Скачай: https://ffmpeg.org/download.html")
    if not SRC.exists():
        print(f"WARN: {SRC} не существует — пропускаю.")
        return

    errors, by_trigger = [], {}
    for f in sorted(SRC.rglob("*")):
        if f.is_dir() or f.suffix.lower() not in {".mp3", ".wav", ".ogg"}:
            continue
        if not NAME_RE.match(f.stem):
            errors.append(f"Невалидное имя: {f.name}")
            continue
        trigger, variant = parse_name(f.stem)
        if trigger not in TRIGGERS:
            errors.append(f"Неизвестный триггер '{trigger}' в {f.name}")
            continue
        slug = trigger.replace(".", "/")
        if variant:
            slug += "_" + variant
        convert(f, DST_SOUNDS / (slug + ".ogg"))
        by_trigger.setdefault(trigger, []).append("grimoiremod:" + slug)

    if errors:
        for e in errors: print(f"FAIL: {e}", file=sys.stderr)
        sys.exit(1)

    out = {t: {"category": TRIGGERS[t], "sounds": sorted(s)} for t, s in by_trigger.items()}
    JSON_OUT.parent.mkdir(parents=True, exist_ok=True)
    JSON_OUT.write_text(json.dumps(out, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"OK: {len(by_trigger)} звуковых событий -> {JSON_OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
