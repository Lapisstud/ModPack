# Git-воркфлоу для команды Grimoire

## Первичная настройка (один раз)

~~~bash
# Требуется: git, git-lfs, python 3.10+, ffmpeg
git lfs install
bash tools/setup_git.sh
~~~

## Ветки

| Ветка | Куда попадает | Куда деплоится |
|---|---|---|
| `main` | Релизы (теги `v*`) | prod-сервер |
| `test` | Все принятые фичи | test-сервер |
| `feature/<name>` | Твоя работа | — |
| `hotfix/<name>` | Срочный фикс | — |
| `assets/<name>` | Работа артистов (без кода) | `test` |

## Поток фичи

~~~bash
git checkout -b feature/my-feature
# работа
git add <файлы>
git commit -m "feat: краткое описание"
git push -u origin feature/my-feature
# PR -> test, зелёный CI + ревью
~~~

## Поток ассетов

См. `docs/for-artists.md`. Коротко:

~~~bash
git checkout -b assets/book-sounds
# положить .mp3/.png/.bbmodel в assets-src/
python tools/validate_assets.py    # OK: ассеты валидны
python tools/build_sounds.py       # сгенерить resourcepack
git add assets-src/ modpack/resourcepacks/
git commit -m "assets: book.use sound"
git push -u origin assets/book-sounds
~~~

## Что НЕ коммитить

- `modpack/mods/*.jar` — через `manifest.json` (генерируется CI)
- сгенерированные `.ogg`/`sounds.json` — из `assets-src/`
- `modpack/manifest.json`
- `.patch_backup_*/`, `run/`, `build/`, `*.log`

Всё это в `.gitignore`. Если что-то оказалось в `git status` — правило сломалось.

## Сообщения коммитов

`<тип>: <описание>`. Типы: `feat`, `fix`, `assets`, `docs`, `refactor`, `chore`.

## Защита веток

GitHub -> Settings -> Branches:
- `main`: PR + 1 review + no force-push
- `test`: PR + CI зелёный
