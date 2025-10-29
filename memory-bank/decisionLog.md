# Decision Log

This file records architectural and implementation decisions using a list format.
2025-09-03 10:33:44 - Log of updates made.

*

## Decision

* **[2025-09-03 10:33:44]** - Принято решение о первоначальном заполнении Memory Bank на основе автоматического анализа
  файловой структуры проекта.

## Rationale

* Необходимо создать базовый контекст о проекте для всех последующих операций и режимов работы. Автоматический анализ
  позволяет быстро получить высокоуровневое представление о технологическом стеке, архитектуре и назначении проекта без
  необходимости задавать вопросы пользователю.

## Implementation Details

* Были проанализированы имена файлов и директорий, зависимости (Gradle), конфигурационные
  файлы (`Dockerfile`, `prometheus.yml`) и существующий код.
* На основе этого анализа были заполнены разделы "Project Goal", "Key Features" и "Overall Architecture"
  в `productContext.md`, а также другие файлы Memory Bank.

[2025-09-12 20:03:47] - Принято решение о добавлении функции отображения номеров игроков, проголосовавших за
заголосованного игрока.

## Decision

* Добавить новое поле `votedBy` в модель `GamePlayer` для хранения информации о голосовавших игроках
* Разместить номера голосовавших под фото игрока, над именем (Вариант 1)
* Использовать существующую систему цветовых бейджей с адаптацией под новые требования

## Rationale

* Повышение информативности оверлеев - зрители смогут видеть, кто именно проголосовал за каждого игрока
* Выбранное расположение не загораживает важную информацию (фото, имя) и логично связано с игроком
* Использование существующих паттернов дизайна обеспечивает консистентность интерфейса

## Implementation Details

* Бэкенд: Новое поле `votedBy: MutableList<Map<String, String>>?` в `GamePlayer.kt`
* Фронтенд: HTML-контейнер `.voted-by-container` с бейджами `.voted-by-badge`
* Стили: Компактные номера (18px высота) с цветовой кодировкой по ролям
* Анимации: Плавное появление с задержкой между номерами
* Условия отображения: Только для игроков со статусом "voted"

[2025-09-18 16:10:25] - Принято решение об изменении логики формирования названий игр в Polemica для улучшения
информативности.

## Decision

* Изменить формирование названий игр в PolemicaService для включения дополнительной информации:
    - Номер стола (если в фазе турнира больше одного стола)
    - Маркер "Финал" (при phase=2)
* Создать вспомогательные функции `getTablesCountInPhase()` и `generateGameTitle()`
* Модифицировать метод `createGameFromPolemica()` в строке 154

## Rationale

* Повышение информативности названий игр для зрителей и участников
* Четкое различение игр на разных столах в многостольных фазах
* Выделение финальных игр специальным маркером
* Сохранение обратной совместимости с существующим форматом

## Implementation Details

* **Новый формат названий:**
    - Одиночный стол: `"Турнир | Игра N"`
    - Несколько столов: `"Турнир | Игра N | Стол M"`
    - Финал: `"Турнир | Финал | Игра N"` или `"Турнир | Финал | Игра N | Стол M"`
* **Функция подсчета столов:** Использует `gameRepository.findGamesByTournamentId()` с фильтрацией по фазе
* **Место изменений:** `src/main/kotlin/com/stoum/overlay/service/polemica/PolemicaService.kt`

[2025-09-24 23:37:09] - Принято решение о реализации продвинутой системы обработки ошибок краулинга в PolemicaService с
различением типов ошибок и автоматическим восстановлением.

## Decision

* Расширить модель Game полями для отслеживания ошибок краулинга (crawlFailureCount, lastCrawlError, lastFailureTime,
  crawlStopReason)
* Реализовать детальную обработку различных типов ошибок:
    - HTTP 404 (игра удалена) - немедленная остановка краулинга
    - HTTP 401/403 (проблемы авторизации) - остановка после 5 попыток
    - Сетевые ошибки - остановка после 3 попыток
    - Неизвестные ошибки - остановка после 2 попыток
* Добавить методы восстановления краулинга (ручное и автоматическое)
* Реализовать административные методы для мониторинга проблемных игр

## Rationale

* Различение типов ошибок позволяет применять разные стратегии обработки
* Счетчик попыток предотвращает бесконечные попытки краулинга проблемных игр
* Автоматическое восстановление для временных проблем (сеть) повышает надежность
* Детальное логирование и статистика упрощают диагностику проблем
* Административные методы обеспечивают контроль и мониторинг системы

## Implementation Details

* Модифицированы файлы:
    - `src/main/kotlin/com/stoum/overlay/entity/Game.kt` - добавлены новые поля
    - `src/main/kotlin/com/stoum/overlay/repository/GameRepository.kt` - новые методы поиска
    - `src/main/kotlin/com/stoum/overlay/service/polemica/PolemicaService.kt` - улучшенная обработка ошибок
* Добавлены методы: handleCrawlError(), restartGameCrawling(), autoRecoverStoppedGames(), getCrawlErrorStatistics(),
  getProblematicGames()
* Реализована логика сброса счетчика ошибок при успешном краулинге

[2025-10-09 17:26:56] - Добавлена новая фича "Факты об игроках" в систему оверлеев. Принято решение создать отдельную
сущность Fact с полями: factText, playerNickname, stage, displayTimeSeconds и связью @ManyToOne с
TournamentOverlaySettings. Это позволяет хранить множество фактов для каждого турнира и управлять ими через
админ-панель.

[2025-10-09 17:26:56] - Создан новый контроллер TournamentSettingsAdminController для управления настройками турнира и
фактами. Выбран RESTful подход с эндпоинтами: GET для отображения страницы, POST для добавления фактов, DELETE для
удаления. Это обеспечивает четкое разделение ответственности и удобный API.

[2025-10-09 17:26:56] - Добавлен HTML-интерфейс tournament_settings.html с использованием Bootstrap 5 для управления
фактами. Интерфейс включает форму добавления фактов с выбором игрока из участников турнира, указанием стадии показа и
времени отображения. Это обеспечивает удобное управление фактами через веб-интерфейс.

[2025-10-28 14:11:18] - Принято решение о миграции JSON-строковых колонок на jsonb через Flyway

## Decision

- Подключить Flyway и добавить транзакционную
  миграцию [V20251028__jsonb_migration.sql](src/main/resources/db/migration/V20251028__jsonb_migration.sql) без индексов
  на данном этапе.
- Перевести следующие колонки из текстового JSON в jsonb:
  - Таблица game_player: checks, guess, voted_by, stat (
    см. [GamePlayer.kt](src/main/kotlin/com/stoum/overlay/entity/overlay/GamePlayer.kt:1);
    поля: [checks](src/main/kotlin/com/stoum/overlay/entity/overlay/GamePlayer.kt:30), [guess](src/main/kotlin/com/stoum/overlay/entity/overlay/GamePlayer.kt:32), [voted_by](src/main/kotlin/com/stoum/overlay/entity/overlay/GamePlayer.kt:34), [stat](src/main/kotlin/com/stoum/overlay/entity/overlay/GamePlayer.kt:36))
  - Таблица game: vote_candidates (см. [Game.kt](src/main/kotlin/com/stoum/overlay/entity/Game.kt:1);
    поле: [voteCandidates](src/main/kotlin/com/stoum/overlay/entity/Game.kt:55))
- Установить DEFAULT значения: [] для списков (checks, guess, voted_by, vote_candidates) и {} для stat.
- Обеспечить отсутствие даунтайма: выполнение в одной транзакции; Flyway на PostgreSQL обеспечивает транзакционность по
  умолчанию.

## Rationale

- jsonb даёт:
  - типобезопасность и валидацию JSON на уровне БД;
  - поддержку JSON-операторов и путевых запросов;
  - снижение оверхеда за счёт отказа от кастомных конвертеров (AttributeConverter);
  - лучшую производительность и возможности индексации (GIN) при необходимости в будущем.

## Implementation Details

- Добавить зависимость Flyway в [build.gradle](build.gradle) (пример): implementation "org.flywaydb:flyway-core:
  &lt;latest&gt;".
- Включить Flyway в конфигурации приложения: добавить свойства в [
  `application.properties`](src/main/resources/application.properties:1) (spring.flyway.enabled=true;
  spring.flyway.clean-disabled=true; при существующей схеме — spring.flyway.baseline-on-migrate=true). Рекомендуется
  перевести управление схемой на Flyway (spring.jpa.hibernate.ddl-auto=none) на проде.
- Создать [V20251028__jsonb_migration.sql](src/main/resources/db/migration/V20251028__jsonb_migration.sql) со следующей
  логикой:
  - Условительные переименования на случай camelCase колонок:
    - game.voteCandidates → vote_candidates
    - game_player.votedBy → voted_by
  - Приведение типов:
    - game_player.checks/guess/voted_by: ALTER COLUMN ... TYPE jsonb USING CASE WHEN col IS NULL OR trim(col) = ''
      THEN '[]'::jsonb ELSE col::jsonb END; затем SET DEFAULT '[]'::jsonb
    - game_player.stat: ALTER COLUMN stat TYPE jsonb USING CASE WHEN stat IS NULL OR trim(stat) = '' THEN '{}'::jsonb
      ELSE stat::jsonb END; затем SET DEFAULT '{}'::jsonb
    - game.vote_candidates: аналогично спискам (DEFAULT '[]'::jsonb)
- Обновить сущности, убрав конвертеры и установив jsonb-аннотации по
  образцу [TournamentUsageLog.tables](src/main/kotlin/com/stoum/overlay/entity/TournamentUsageLog.kt:23):
  - В [GamePlayer.kt](src/main/kotlin/com/stoum/overlay/entity/overlay/GamePlayer.kt:1)
    и [Game.kt](src/main/kotlin/com/stoum/overlay/entity/Game.kt:1) для указанных полей поставить @JdbcTypeCode(
    SqlTypes.JSON) + @Column(columnDefinition = "jsonb"), удалить @Convert и импорты конвертеров.
- Удалить неиспользуемые
  конвертеры [MapListConverter.kt](src/main/kotlin/com/stoum/overlay/entity/converters/MapListConverter.kt:1)
  и [MapMapConverter.kt](src/main/kotlin/com/stoum/overlay/entity/converters/MapMapConverter.kt:6) после рефакторинга.
