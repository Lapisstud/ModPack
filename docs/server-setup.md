# Настройка тестового сервера

Для hosting-minecraft или любого NeoForge-хостинга.

## Параметры

| Параметр | Значение |
|---|---|
| Java Edition | **1.21.1** |
| Java | **21** |
| Загрузчик | **NeoForge 21.1.218** |
| RAM | 4–8 GB |
| Диск | >= 20 GB SSD |

## Установка

1. **NeoForge**. Если панель не поддерживает:
   - Загрузи `neoforge-21.1.218-installer.jar` в корень сервера
   - `java -jar neoforge-21.1.218-installer.jar --installServer`
   - Startup Command: `java -Xmx4G -jar @libraries/net/neoforged/neoforge/21.1.218/unix_args.txt nogui`

2. **`eula.txt`** -> `eula=true`

3. **`server.properties`**:
   ~~~properties
   online-mode=true
   motd=\u00A76Grimoire [TEST]
   max-players=10
   view-distance=8
   simulation-distance=6
   spawn-protection=0
   white-list=true
   ~~~

4. **`whitelist.json`** / **`ops.json`** — через `/whitelist add`, `/op`.

5. **`mods/`** — `grimoiremod-1.0.0.jar` из `build/libs/` + сторонние моды.

6. **`config/`** — пока пустой.

## Проверка

1. Запусти сервер.
2. Зайди в игру (тот же мод в клиенте).
3. `/give @s grimoiremod:grimoire` — появился гримуар.
4. `/glyphtest fireball 5` — fireball в первом слоте.
5. ПКМ — открывается экран.

## Бэкапы

- Автобэкап daily в панели.
- 3 последних минимум.
- Ручной перед каждым обновлением мода.

## Ручной деплой (до CI)

1. Локально: `./gradlew build` -> `build/libs/grimoiremod-1.0.0.jar`
2. SFTP/FTP -> `mods/`
3. Рестарт сервера.
