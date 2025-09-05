# System Patterns *Optional*

This file documents recurring patterns and standards used in the project.
It is optional, but recommended to be updated as the project evolves.
2025-09-03 10:33:31 - Log of updates made.

*

## Coding Patterns

* **Kotlin & Spring Boot:** Использование `data class` для моделей и DTO, `extension functions` для расширения
  функциональности, активное применение Spring аннотаций (`@RestController`, `@Service`, `@Repository`, `@Autowired`).
* **JPA/Hibernate:** Использование аннотаций `@Entity`, `@Id`, `@GeneratedValue`, `@ManyToOne` и т.д., для маппинга
  объектов на таблицы БД. Репозитории наследуются от `JpaRepository`.
* **Converters:** Применение `AttributeConverter` для кастомной сериализации сложных типов данных в БД (
  например, `MapListConverter`, `MapMapConverter`).

## Architectural Patterns

* **Layered Architecture (N-tier):** Четкое разделение на слои:
    * `controller` (обработка HTTP запросов)
    * `service` (бизнес-логика)
    * `repository` (доступ к данным)
    * `entity` (модели данных)
* **Repository Pattern:** Абстрагирование логики доступа к данным за интерфейсами репозиториев (Spring Data).
* **RESTful API:** Проектирование API для взаимодействия с клиентами и внешними системами.
* **Server-Sent Events (SSE):** Использование для отправки обновлений с сервера на клиент в реальном времени.

## Testing Patterns

* **Unit Testing:** Наличие `PlayerServiceTest.kt` указывает на использование фреймворков для юнит-тестирования (
  вероятно, JUnit 5 и Mockito/MockK) для проверки логики сервисного слоя в изоляции.
