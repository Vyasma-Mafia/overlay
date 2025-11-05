# Overlay (RU)

English version: [README.md](README.md)

Overlay — это приложение на Kotlin/Spring Boot для:
- отображения оверлеев в реальном времени для трансляций игр в спортивную мафию (через SSE),
- администрирования турниров, игроков, фотографий и настроек оверлея,
- интеграции с внешними источниками: Polemica и Gomafia.

Фронтенд-страницы рендерятся через Thymeleaf. Данные хранятся в PostgreSQL с миграциями Flyway. Фотографии игроков — в S3-совместимом хранилище. Метрики — в Prometheus.

## Основные возможности

- Оверлеи для трансляций с живыми обновлениями (SSE)
- Админ-панель: турниры, игроки, фото, включение/выключение оверлея
- Интеграции: Polemica, Gomafia
- Панель управления и селектор ролей
- «Факты об игроках» (настройка показа по стадиям игры) с веб-UI
- Prometheus метрики через Spring Boot Actuator
- Поддержка Docker/Docker Compose

## Технологический стек

- Kotlin + Spring Boot 3.4 (Web, Thymeleaf, Data JPA/JDBC, Validation, Actuator)
- PostgreSQL 16 + Flyway
- Server-Sent Events (SSE)
- S3-совместимый сторедж (например, Yandex Object Storage)
- Prometheus
- Gradle; JDK 21

## Структура и ключевые файлы

- Точка входа: [OverlayApplication.kt](src/main/kotlin/com/stoum/overlay/OverlayApplication.kt)
- Контроллеры:
  - Оверлей/контроль/селектор ролей: [OverlayController.kt](src/main/kotlin/com/stoum/overlay/controller/OverlayController.kt)
  - SSE: [SseController.kt](src/main/kotlin/com/stoum/overlay/controller/SseController.kt)
  - Управление состоянием игры: [GameController.kt](src/main/kotlin/com/stoum/overlay/controller/GameController.kt)
  - Админ по фото/игрокам/настройкам оверлея: [PhotoAdminController.kt](src/main/kotlin/com/stoum/overlay/controller/PhotoAdminController.kt)
  - Факты об игроках: [GameFactsAdminController.kt](src/main/kotlin/com/stoum/overlay/controller/GameFactsAdminController.kt)
  - Список/поиск игр: [AdminController.kt](src/main/kotlin/com/stoum/overlay/controller/AdminController.kt), [GameListAdminController.kt](src/main/kotlin/com/stoum/overlay/controller/GameListAdminController.kt)
  - Форс‑перекраулинг Polemica (если включено): [PolemicaController.kt](src/main/kotlin/com/stoum/overlay/controller/PolemicaController.kt)
- Сервисы:
  - Отправка SSE: [EmitterService.kt](src/main/kotlin/com/stoum/overlay/service/EmitterService.kt)
  - Настройки оверлея турнира: [TournamentOverlayService.kt](src/main/kotlin/com/stoum/overlay/service/TournamentOverlayService.kt)
- Конфигурации:
  - Приложение: [ApplicationConfig.kt](src/main/kotlin/com/stoum/overlay/config/ApplicationConfig.kt)
  - Объектное хранилище: [ObjectStorageConfig.kt](src/main/kotlin/com/stoum/overlay/config/ObjectStorageConfig.kt)
  - Jackson: [JacksonConfiguration.kt](src/main/kotlin/com/stoum/overlay/config/JacksonConfiguration.kt)
  - Безопасность: [SecurityConfig.kt](src/main/kotlin/com/stoum/overlay/config/SecurityConfig.kt)
- Данные/миграции: каталог [src/main/resources/db/migration](src/main/resources/db/migration)
- Контейнеризация: [Dockerfile](Dockerfile), [docker-compose.yml](docker-compose.yml)

## Требования

- JDK 21
- Docker и Docker Compose (рекомендуется) или локальный PostgreSQL 16
- Доступ к S3-совместимому объектному хранилищу (ключ/секрет)

## Быстрый старт (Docker Compose)

1) Создайте файл `.env` в корне (см. ниже пример или используйте [.env.example](.env.example), когда он появится).
2) Запустите:

```bash
docker compose up -d
```

3) Доступ к сервисам:
- Приложение: http://localhost:8090
- База данных: порт 5435 (host) → 5432 (container)
- Prometheus: http://localhost:9091

Конфиги: [docker-compose.yml](docker-compose.yml), [prometheus.yml](prometheus.yml).

## Локальная разработка (без контейнеров)

1) Поднимите PostgreSQL и создайте БД `overlay`.
2) Экспортируйте переменные окружения (минимум):

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/overlay
export DATABASE_USER=postgres
export DATABASE_PASSWORD=postgres
export OVERLAY_ADMIN_PASSWORD=change-me
export S3_KEY_ID=...
export S3_SECRET_ACCESS_KEY=...
```

3) Запуск приложения:

```bash
./gradlew bootRun
```

Либо сборка и запуск jar:

```bash
./gradlew build
java -jar build/libs/app.jar
```

Dockerfile использует двухфазную сборку: [Dockerfile](Dockerfile).

## Конфигурация и переменные окружения

Базовые настройки в [application.properties](src/main/resources/application.properties). Важные параметры:

- Подключение к БД: `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
- Пароль админа оверлея: `OVERLAY_ADMIN_PASSWORD` (используется сервисом настроек оверлея)
- S3: `S3_KEY_ID`, `S3_SECRET_ACCESS_KEY`, а также `s3.region`, `s3.endpoint`, `s3.bucket.name`
- Флаги приложения: `app.polemicaEnable`, `app.crawlScheduler.enable`, `app.crawlScheduler.interval`
- Actuator/Prometheus на 8081 через `management.*`

Шаблон `.env` будет добавлен в [.env.example](.env.example).

## Маршруты UI

- Оверлей:
  `/{service}/tournaments/{tournamentId}/phases/{phase}/tables/{tableNum}/games/{gameNum}/overlay`
- Панель управления:
  `/{service}/tournaments/{tournamentId}/phases/{phase}/tables/{tableNum}/games/{gameNum}/control`
- Селектор ролей:
  `/{service}/tournaments/{tournamentId}/phases/{phase}/tables/{tableNum}/games/{gameNum}/roleselector`

Где `service ∈ {polemica, gomafia}`. Реализация: [OverlayController.kt](src/main/kotlin/com/stoum/overlay/controller/OverlayController.kt).

## Реальное время (SSE)

- Канал оверлея: `GET /{id}/gameinfo`
- Канал панели: `GET /{id}/controlinfo`
- Канал селектора ролей: `GET /{id}/roleselectorinfo`

Реализация: [SseController.kt](src/main/kotlin/com/stoum/overlay/controller/SseController.kt), отправка — [EmitterService.kt](src/main/kotlin/com/stoum/overlay/service/EmitterService.kt).

## Основные REST-эндпоинты

Работа с игрой (UUID в пути):
- `POST /{id}/game` — отправить снапшот GameInfo на клиентов
- `POST /{id}/roles` — массовая установка ролей (place→role)
- `POST /{id}/status` — массовая установка статусов
- `POST /{id}/setSpeaker?playerNum=...`
- `POST /{id}/visibleOverlay|visibleRoles|visibleScores?value=Boolean`
- `POST /{id}/started|text|delay|autoNextGame`
- `POST /{id}/resetStatuses`, `POST /{id}/resetRoles`
- `POST /{id}/setPlayerChecks`, `POST /{id}/setPlayerGuesses`

Админ:
- Фото/игроки/турниры — пространство `/admin/photos` (см. [PhotoAdminController.kt](src/main/kotlin/com/stoum/overlay/controller/PhotoAdminController.kt))
- Факты об игроках — `/admin/games` (см. [GameFactsAdminController.kt](src/main/kotlin/com/stoum/overlay/controller/GameFactsAdminController.kt))
- Поиск/список игр — см. [AdminController.kt](src/main/kotlin/com/stoum/overlay/controller/AdminController.kt), [GameListAdminController.kt](src/main/kotlin/com/stoum/overlay/controller/GameListAdminController.kt)
- Форс-перекраулинг Polemica (если включено): `POST /polemica/_force_recheck` (см. [PolemicaController.kt](src/main/kotlin/com/stoum/overlay/controller/PolemicaController.kt))

## Swagger UI

Подключен springdoc. URL по умолчанию:
- http://localhost:8090/swagger-ui/index.html

Примечание: не все эндпоинты могут быть полностью описаны аннотациями.

## База данных и миграции

Миграции включены (Flyway), файлы в каталоге [src/main/resources/db/migration](src/main/resources/db/migration).

## Мониторинг

- Actuator на порту 8081, метрики Prometheus на `/metrics` (см. [application.properties](src/main/resources/application.properties))
- Конфиг Prometheus: [prometheus.yml](prometheus.yml)

## Безопасность

Текущая [SecurityConfig.kt](src/main/kotlin/com/stoum/overlay/config/SecurityConfig.kt) отключает Basic/CORS/CSRF — предполагается запуск в доверенной сети/за реверс‑прокси. Изменение доступности оверлея защищено паролем (`OVERLAY_ADMIN_PASSWORD`) в сервисе настроек оверлея.

Рекомендации для продакшена:
- ограничить доступ к `/admin/*` сетевыми правилами/прокси-авторизацией,
- включить аутентификацию/роли в приложении,
- ограничить доступ к изменяющим POST-эндпоинтам.

## Сборка и тесты

```bash
./gradlew build
./gradlew test
```

## Лицензия

Apache-2.0 — см. [LICENSE.md](LICENSE.md).