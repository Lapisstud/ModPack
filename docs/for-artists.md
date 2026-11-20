# Инструкция для артистов и билдеров

Для тех, кто **не пишет код**, но создаёт **звуки, текстуры, модели и структуры**.

Всё кладётся в `assets-src/`. Скрипты раскладывают куда надо.

---

## Звуки

### Куда класть
`assets-src/sounds/` — можно по подпапкам.

### Как называть
**`<триггер>.<вариант>.mp3`** или `.wav` / `.ogg`.

- `<триггер>` — из таблицы ниже. Определяет, **когда** звук заиграет.
- `<вариант>` — необязательно. **Одна цифра 1–9**. Несколько вариантов одного триггера — игра чередует случайно.
- Только нижний регистр. Без пробелов, без кириллицы.

**Примеры:** `book.use.mp3`, `book.use.1.mp3` + `book.use.2.mp3`, `spell.hit.fire.ogg`.

### Триггеры (v1)

| Триггер | Когда |
|---|---|
| `book.use` | ПКМ книгой в руке |
| `book.open` / `book.close` | Открытие / закрытие экрана |
| `book.page` | Перелистывание |
| `book.error` | Ошибка заклинания |
| `glyph.select` / `glyph.place` / `glyph.remove` | Взаимодействие с глифами |
| `glyph.point` | Кнопки `+`/`-` |
| `workbench.open` / `workbench.close` | Верстак |
| `skilltree.open` / `skilltree.unlock` | Дерево навыков |
| `spell.cast` | Общий каст |
| `spell.cast.projectile` / `.self` / `.touch` | Каст по форме |
| `spell.hit.fire` / `.ice` / `.water` / `.heal` | Попадание |
| `spell.impact.block` | Попадание в блок |
| `ui.click` | Клик UI |

Нет нужного триггера — **спроси координатора**, не выдумывай.

### Проверка перед коммитом
~~~bash
python tools/validate_assets.py
python tools/build_sounds.py
~~~

---

## Текстуры и модели (Blockbench)

- Текстуры: `assets-src/textures/<item|block>/<name>.png`
- Модели: `assets-src/models/<item|block>/<name>.bbmodel`

**Правила:**
- Имя: `<name>.png`, только `[a-z0-9._-]`.
- PNG: стороны = степень двойки (16/32/64/128), макс 512×512.
- Blockbench -> экспорт **Java Block/Item Model**, не Bedrock.
- Анимации: `.animation.json` рядом.

Проверка: `python tools/validate_assets.py`

---

## Структуры (данжи)

**Structure Block:** построй -> Save -> `.nbt` из папки мира -> `assets-src/structures/<данж>/<комната>.nbt`.

**Litematica:** выдели -> сохрани -> `.litematic` -> `assets-src/structures/<данж>/<комната>.litematic`.

**Правила:**
- Имя: `<комната>.nbt`, только `[a-z0-9._-]`.
- Подпапка `<данж>` = будущий ID (`crypt`, `tower`, ...).
- Не пустой.

**Тест в мире** после сборки мода:
~~~
/place template grimoiremod:crypt/entrance
~~~

---

## Если что-то сломалось

| Симптом | Что делать |
|---|---|
| `FAIL: [имя файла]` | Переименуй: латиница нижнего регистра, цифры, `._-` |
| `FAIL: Неизвестный триггер` | Сверься с таблицей. Нет — напиши координатору |
| `ffmpeg не найден` | https://ffmpeg.org/download.html |
| Git ругается на большой файл | `git lfs install` (один раз) |
