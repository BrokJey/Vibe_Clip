## Vibe Clip Backend Architecture

### Vision
Vibe Clip — экспериментальная соцсеть с короткими видео, где каждый пользователь может создавать несколько независимых «папок»-лент с отдельными алгоритмами рекомендаций. Kotlin-приложение (Android) общается с REST API, написанным на Java (Spring Boot + Hibernate) с использованием Maven, Liquibase и JWT.

### Технологический стек
- Spring Boot 3.1 (Web, Data JPA, Security)
- Hibernate/JPA как ORM
- Liquibase для миграций
- PostgreSQL (production) / H2 (dev)
- JJWT для подписи токенов
- MapStruct (позже) / ручные мапперы для DTO
- Lombok опционально для сущностей/DTO

### Пакетная структура (`com.vibeclip`)
- `config` — Beans, кросс-срезочные настройки (CORS, WebMvc, OpenAPI).
- `controller` — REST-контроллеры, отвечают DTO и статусами.
- `dto` — `request`/`response` наборы, record/классы без JPA-аннотаций.
- `entity` — JPA-сущности + enum-типизации.
- `mapper` — конвертация `entity ↔ dto`.
- `repository` — интерфейсы Spring Data JPA, кастомные query-методы.
- `security` — фильтры, JWT-провайдер, `SecurityConfig`, модели ролей.
- `service` — бизнес-логика, транзакции, работа с внешними сервисами.

### Первичные доменные сущности
| Сущность        | Назначение |
|-----------------|------------|
| `User`          | Аккаунт, содержит email/username, статус, список ролей. |
| `Role`          | Перечисление `USER`, `CREATOR`, `ADMIN`. |
| `Folder`        | Пользовательская лента. Связана с владельцем, хранит настройки. |
| `FolderPreference` | JSON/embeddable набор правил рекомендаций (хэштеги, авторы). |
| `Video`         | Мета-информация о ролике (url, длительность, владелец). |
| `VideoMetric`   | Аггрегированные показатели (просмотры, лайки). |
| `FolderVideo`   | Many-to-many между `Folder` и контентом, с позицией/score. |
| `Reaction`      | Лайк/досмотр/репорт для обучения рекомендаций. |
| `RefreshToken`  | Опционально, если нужен refresh-flow. |

### Типовой REST API (черновик)
| Endpoint | Метод | Роль | Кратко |
|----------|-------|------|--------|
| `/api/v1/auth/register` | POST | anon | Регистрация, email + пароль. |
| `/api/v1/auth/login` | POST | anon | Возврат access/refresh JWT. |
| `/api/v1/folders` | GET | USER | Список папок текущего пользователя. |
| `/api/v1/folders` | POST | USER | Создание папки с настройками. |
| `/api/v1/folders/{id}/feed` | GET | USER | Получение ленты с пагинацией. |
| `/api/v1/videos` | POST | CREATOR | Публикация ролика. |
| `/api/v1/videos/{id}/reactions` | POST | USER | Лайк/дизлайк/просмотр. |
| `/api/v1/admin/moderation/videos` | GET | ADMIN | Проверка репортов. |

### Безопасность
- JWT access-токен (~15 мин) + опциональный refresh.
- `SecurityFilterChain`:
  1. `JwtAuthenticationFilter` извлекает токен из `Authorization: Bearer`.
  2. Делегирует `JwtService` для валидации подписи и срока.
  3. Создаёт `UsernamePasswordAuthenticationToken` с ролями пользователя.
- Пароли через `BCryptPasswordEncoder`.
- Роли/правила задаются `@PreAuthorize` и matcher'ами в конфиге.

### Liquibase
- `src/main/resources/db/changelog/db.changelog-master.yaml`.
- Первые changeSet'ы: таблицы пользователей, ролей, видеоконтента, папок.
- Дальнейшие миграции описывают эволюцию схемы (новые настройки рекомендаций).

### Алгоритмические заметки
1. Каждая папка хранит собственные фильтры (теги, авторы, гео, длительность).
2. `RecommendationService` собирает кандидатов:
   - быстрый фильтр (SQL) по тегам/бан-листу → 100–200 роликов;
   - ранжирование (регулируемый ML/эвристика) → 20 итоговых.
3. Реакции пользователя пишутся асинхронно (spring events / message queue v2).

### Дорожная карта backend
1. **Базовый foundation**
   - Настроить `application.yml`, профили `dev/prod`.
   - Поднять Liquibase с начальными таблицами пользователей/ролей.
   - Реализовать регистрацию/логин, JWT и защиту эндпоинтов.
2. **Контент**
   - Сущности видео, папок, связи.
   - CRUD папок и загрузка видео (пока метаданные + S3 stub).
3. **Рекомендации**
   - Сервисы формирования ленты, кеширование, реакций.
4. **Экосистема**
   - Нотификации, модерация, аналитика, интеграция с Kotlin-клиентом.

Документ будет расширяться по мере появления деталей фронтенда и бизнес-логики.

