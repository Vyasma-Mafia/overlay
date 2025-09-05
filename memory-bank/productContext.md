# Product Context

This file provides a high-level overview of the project and the expected product that will be created. Initially it is
based upon projectBrief.md (if provided) and all other available project-related information in the working directory.
This file is intended to be updated as the project evolves, and should be used to inform all other modes of the
project's goals and context.
2025-09-03 10:32:07 - Log of updates made will be appended as footnotes to the end of this file.

*

## Project Goal

* Разработка и поддержка веб-приложения "Overlay" для спортивной мафии. Приложение предоставляет оверлеи для трансляций
  игр, админ-панель для управления турнирами, игроками и контентом, а также интеграцию со сторонними сервисами (
  gomafia.pro, polemica.app).

## Key Features

* **Оверлеи для трансляций:** Отображение игровой информации в реальном времени (состав стола, роли, голосования,
  состояние игроков).
* **Административная панель:**
    * Управление турнирами и их настройками.
    * Управление профилями игроков (включая фотографии).
    * Управление играми и их ходом.
    * Поиск и просмотр информации по игрокам.
* **Интеграция с Gomafia.pro:** Синхронизация данных о турнирах, игроках и играх.
* **Интеграция с Polemica.app:** Получение данных с сервиса Полемики.
* **Real-time обновления:** Использование Server-Sent Events (SSE) для мгновенной доставки обновлений на клиентские
  интерфейсы.
* **Система ролей:** Функционал для выбора ролей в игре.

## Overall Architecture

* **Бэкенд:** Spring Boot приложение на языке Kotlin.
* **Сборка:** Gradle.
* **База данных:** Реляционная БД (предположительно PostgreSQL, судя по зависимостям в Spring), взаимодействие через
  Spring Data JPA.
* **Фронтенд:** Шаблонизатор Thymeleaf для генерации HTML-страниц.
* **API:** REST-контроллеры для взаимодействия с админ-панелью и внешними системами.
* **Контейнеризация:** Наличие `Dockerfile` и `docker-compose.yml` указывает на использование Docker для развертывания.
* **Мониторинг:** Наличие `prometheus.yml` указывает на интеграцию с системой мониторинга Prometheus.
