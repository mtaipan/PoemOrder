# PoemOrder — сайт заказа стихов

Небольшой сайт-визитка автора стихов на заказ: публичная витрина (главная, портфолио, цены, отзывы, контакты), форма заявки и закрытая админ-панель для управления контентом.

**Стек:** Java 21 · Spring Boot 4 (Web MVC, Security, Data JPA, Validation, Actuator) · PostgreSQL 16 · Flyway · Thymeleaf · Docker.

---

## Содержание

1. [Возможности](#возможности)
2. [Быстрый старт](#быстрый-старт)
3. [Переменные окружения](#переменные-окружения)
4. [Структура проекта](#структура-проекта)
5. [Архитектура](#архитектура)
6. [Маршруты](#маршруты)
7. [База данных и миграции](#база-данных-и-миграции)
8. [Безопасность](#безопасность)
9. [Антиспам](#антиспам)
10. [Telegram-уведомления](#telegram-уведомления)
11. [Тестирование](#тестирование)
12. [Деплой](#деплой)
13. [Известные ограничения и планы](#известные-ограничения-и-планы)

---

## Возможности

### Публичная часть
- **Главная** — hero-блок (заголовок/подзаголовок редактируются в админке), последние опубликованные работы, последние одобренные отзывы.
- **Портфолио** — список опубликованных стихов с отрывками и страница полного текста.
- **Цены и условия** — текстовые блоки «оплата / сроки / возврат», редактируются в админке.
- **Отзывы** — публичная форма; отзыв попадает в очередь модерации и виден только после одобрения. Можно указать Telegram-username и разрешить/запретить его показ.
- **Контакты** — карточки контактов (label / значение / ссылка), управляются из админки.
- **Заявка на стих** — форма заказа (имя, телефон, соцсеть, описание).

### Админ-панель (`/admin`)
- **Dashboard** — стартовая страница.
- **Стихи** — CRUD, статусы «Черновик / Опубликовано».
- **Заказы** — список заявок, карточка заявки, смена статуса (NEW → IN_PROGRESS → DONE / CANCELED).
- **Отзывы** — модерация: одобрить, скрыть контакт, удалить.
- Списки заказов, стихов и опубликованных отзывов пагинированы (по 20 на страницу); очередь модерации показывается целиком.
- **Контакты** — CRUD контактных карточек, сортировка, вкл/выкл.
- **Настройки** — hero-тексты, заголовки портфолио, блоки цен, легаси-контакты.

### Служебное
- Telegram-уведомления о новых заявках и отзывах (опционально, см. ниже).
- Rate limiting публичных форм + honeypot-поле против ботов.
- Health-check: `GET /actuator/health`.

---

## Быстрый старт

### Вариант 1: всё в Docker

```bash
# 1. Создай .env в корне проекта (см. раздел «Переменные окружения»)
cat > .env <<'EOF'
POSTGRES_DB=poems
POSTGRES_USER=poems
POSTGRES_PASSWORD=poems
APP_ADMINS=admin:сильный-пароль
EOF

# 2. Подними всё
docker compose up --build
```

Приложение: http://localhost:8081 · Админка: http://localhost:8081/admin

### Вариант 2: БД в Docker, приложение локально

```bash
# 1. Только база
docker compose up -d db

# 2. Запуск приложения (APP_ADMINS обязателен — без него старт упадёт)
APP_ADMINS=admin:admin ./mvnw spring-boot:run
```

> **Важно:** приложение намеренно отказывается стартовать без `APP_ADMINS` —
> это защита от случайного деплоя админки без учётных записей.

---

## Переменные окружения

| Переменная | Обязательна | По умолчанию | Описание |
|---|---|---|---|
| `APP_ADMINS` | **да** | — | Учётки админов в формате `user:pass,user2:pass2`. Пароли хэшируются Argon2 при старте. |
| `SPRING_DATASOURCE_URL` | нет | `jdbc:postgresql://localhost:5432/poems` | JDBC-URL базы |
| `SPRING_DATASOURCE_USERNAME` | нет | `poems` | Пользователь БД |
| `SPRING_DATASOURCE_PASSWORD` | нет | `poems` | Пароль БД |
| `SERVER_PORT` | нет | `8081` | HTTP-порт приложения |
| `DB_POOL_SIZE` | нет | `10` | Максимум соединений Hikari |
| `DB_POOL_MIN_IDLE` | нет | `2` | Минимум idle-соединений |
| `THYMELEAF_CACHE` | нет | `true` | Кэш шаблонов. Локально ставь `false` для live-reload |
| `TELEGRAM_BOT_TOKEN` | нет | — | Токен бота для уведомлений. Пусто = уведомления выключены |
| `TELEGRAM_CHAT_ID` | нет | — | Chat ID, куда слать уведомления |
| `RATE_LIMIT_MAX` | нет | `5` | Сколько отправок форм разрешено с одного IP за окно |
| `RATE_LIMIT_WINDOW_MINUTES` | нет | `10` | Длина окна rate limit в минутах |

Переменные для docker-compose (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`) задаются в `.env`.
Файл `.env` в `.gitignore` — **никогда не коммить реальные пароли**.

---

## Структура проекта

```
src/main/java/com/poemorder/app/
├── PoemorderApplication.java        # точка входа
├── config/
│   └── SecurityConfig.java          # Spring Security: доступы, форма логина, заголовки
├── domain/                          # JPA-сущности и енумы
│   ├── order/      Order, OrderStatus
│   ├── poem/       Poem, PoemStatus
│   ├── review/     Review, ReviewStatus
│   └── settings/   SiteSettings, ContactLink
├── dto/                             # формы публичной части (валидация)
│   ├── OrderForm.java
│   └── ReviewForm.java
├── repo/                            # Spring Data JPA репозитории
├── service/                         # бизнес-логика, транзакции
│   ├── OrderService.java
│   ├── PoemService.java
│   ├── ReviewService.java
│   ├── ContactLinkService.java
│   └── SiteSettingsService.java
├── integration/telegram/
│   ├── TelegramNotifier.java        # отправка сообщений в Bot API
│   └── TelegramMessageFormatter.java
└── web/
    ├── RateLimitFilter.java         # лимит отправок публичных форм по IP
    ├── admin/                       # контроллеры админки (/admin/**)
    └── public_/                     # публичные контроллеры + GlobalModelAttributes

src/main/resources/
├── application.properties
├── db/migration/                    # Flyway-миграции V1..Vn
├── static/css/                      # public.css, admin.css
└── templates/
    ├── layouts/                     # базовые layout'ы public/admin
    ├── fragments/                   # header, footer, alerts, form-errors
    ├── public/                      # страницы сайта
    ├── admin/                       # страницы админки
    └── error/                       # 403, 404, 500
```

---

## Архитектура

Классическая слоистая архитектура server-side rendered приложения:

```
Браузер
   │  HTTP
   ▼
RateLimitFilter ──► Spring Security ──► Controller (web/)
                                            │ DTO + Bean Validation
                                            ▼
                                        Service (@Transactional)
                                            │
                              ┌─────────────┴─────────────┐
                              ▼                           ▼
                       Repository (JPA)            TelegramNotifier
                              │                     (best-effort,
                              ▼                      не ломает запрос)
                         PostgreSQL
```

Принятые решения:

- **Server-side rendering (Thymeleaf), без JS-фреймворков.** Контента мало, SEO важно, интерактивности почти нет — SPA было бы оверхедом.
- **Schema-first: Flyway владеет схемой**, `spring.jpa.hibernate.ddl-auto=none`. Hibernate никогда не трогает DDL.
- **`open-in-view=false`** — все обращения к БД строго внутри сервисного слоя.
- **Контроллеры тонкие**: публичные формы биндятся в DTO (`OrderForm`, `ReviewForm`) с Bean Validation; маппинг в сущности и сохранение — в сервисах.
- **PRG-паттерн** (Post/Redirect/Get) с flash-атрибутами для всех форм — F5 не дублирует отправку.
- **`GlobalModelAttributes`** (`@ControllerAdvice` только для пакета `public_`) кладёт в модель каждой публичной страницы настройки сайта и контактные карточки.
- **Модерация отзывов**: публичный отзыв всегда создаётся в статусе `PENDING` и не виден, пока админ не одобрит.
- **Уведомления — best effort**: ошибка Telegram API логируется, но не валит сохранение заявки.
- **Кэш настроек**: `site_settings` и контактные карточки читаются на каждом публичном запросе, поэтому кэшируются в памяти (`CacheConfig`); кэш сбрасывается при сохранении из админки.

---

## Маршруты

### Публичные

| Метод | Путь | Что делает |
|---|---|---|
| GET | `/` | Главная |
| GET | `/portfolio` | Список опубликованных стихов |
| GET | `/portfolio/{id}` | Полный текст стиха |
| GET | `/pricing` | Цены и условия |
| GET | `/reviews` | Отзывы + форма |
| POST | `/reviews` | Отправка отзыва (→ модерация) |
| GET | `/contacts` | Контакты |
| GET | `/order` | Форма заявки |
| POST | `/order` | Отправка заявки |
| GET | `/actuator/health` | Health-check |

### Админские (роль `ADMIN`)

| Путь | Раздел |
|---|---|
| `/admin/login`, `/admin/logout` | Вход/выход (form login) |
| `/admin/dashboard` | Dashboard |
| `/admin/poems`, `/admin/poems/new`, `/admin/poems/{id}/edit`, `/admin/poems/{id}/delete` | Стихи |
| `/admin/orders`, `/admin/orders/{id}`, `/admin/orders/{id}/status` | Заказы |
| `/admin/reviews`, `/admin/reviews/{id}/approve`, `…/hide-contact`, `…/delete` | Модерация отзывов |
| `/admin/contacts`, `/admin/contacts/save`, `/admin/contacts/{id}/delete` | Контактные карточки |
| `/admin/settings` | Настройки сайта |

Все остальные пути запрещены по умолчанию (`denyAll`).

---

## База данных и миграции

Схемой управляет Flyway (`src/main/resources/db/migration`). Миграции применяются автоматически при старте.

### Таблицы

| Таблица | Назначение |
|---|---|
| `poem` | Стихи портфолио: title, excerpt, body, status (DRAFT/PUBLISHED), created_at, updated_at |
| `orders` | Заявки: name, phone, social, description, status (NEW/IN_PROGRESS/DONE/CANCELED), created_at |
| `reviews` | Отзывы: name, text, telegram_username, telegram_public, status (PENDING/APPROVED), created_at |
| `site_settings` | Singleton-строка (id=1): hero-тексты, тексты портфолио, блоки цен, легаси-контакты |
| `contact_link` | Контактные карточки: label, value, href, sort_order, enabled |

Индексы покрывают основные выборки: `(status, created_at)` для отзывов, `(status, updated_at desc)` для стихов, `(created_at desc)` и `(status)` для заказов.

### Правила работы с миграциями

- Новая миграция = новый файл `V<n>__<описание>.sql`. Уже применённые файлы **не редактировать**.
- Время храним в `timestamptz`; `hibernate.jdbc.time_zone=UTC` выставлен в конфиге.
- Запись `site_settings` (id=1) создаётся миграцией — приложение рассчитывает, что она есть.

---

## Безопасность

- **Аутентификация** — form login Spring Security, in-memory пользователи из `APP_ADMINS`, пароли хэшируются **Argon2** при старте.
- **Авторизация** — явный allow-list публичных URL; `/admin/**` требует роль `ADMIN`; всё прочее — `denyAll`. Новый эндпоинт по умолчанию закрыт.
- **CSRF включён** для всех форм (Thymeleaf добавляет токен автоматически через `th:action`).
- **Сессии** — защита от session fixation, лимит одновременных сессий, `HttpSessionEventPublisher` для корректного учёта logout.
- **Заголовки** — `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, запрет кэширования ответов админки.
- **Ошибки** — stacktrace и сообщения исключений наружу не отдаются; свои страницы 403/404/500.
- **XSS** — весь пользовательский и админский контент рендерится через `th:text` (экранирование); `th:utext` в проекте не используется. `href` контактных карточек проверяется на допустимую схему (`http/https/mailto/tel`) — `javascript:`-ссылки сохранить нельзя.
- За TLS отвечает reverse proxy (см. «Деплой»); `forward-headers-strategy=framework` учитывает `X-Forwarded-*`.

**Чего здесь нет (осознанно, масштаб не требует):** ролей кроме `ADMIN`, БД-пользователей, 2FA, audit log.

---

## Антиспам

Двухуровневая защита публичных форм (`POST /order`, `POST /reviews`):

1. **Rate limiting** (`RateLimitFilter`) — не более `RATE_LIMIT_MAX` (по умолчанию 5) отправок с одного IP за `RATE_LIMIT_WINDOW_MINUTES` (по умолчанию 10) минут. Сверх лимита — `429 Too Many Requests`. Счётчики в памяти процесса; при рестарте сбрасываются — для одного инстанса этого достаточно.
2. **Honeypot** — скрытое поле `website` в формах. Заполнено → бот: показываем «успех», ничего не сохраняем.

---

## Telegram-уведомления

Уведомления о **новых заявках** и **новых отзывах** приходят в Telegram.

Настройка:
1. Создай бота через [@BotFather](https://t.me/BotFather), получи токен.
2. Узнай свой chat id (например, через [@userinfobot](https://t.me/userinfobot)).
3. Задай `TELEGRAM_BOT_TOKEN` и `TELEGRAM_CHAT_ID` в окружении.

Если токен не задан — функция просто выключена, приложение работает как обычно. Ошибки отправки логируются и не влияют на сохранение заявки/отзыва.

---

## Тестирование

```bash
# Тестам нужен работающий Postgres:
docker compose up -d db
APP_ADMINS=admin:admin ./mvnw test
```

Сейчас есть только smoke-тест поднятия контекста. Планируется перевод на Testcontainers (чтобы тесты сами поднимали БД) и MockMvc-тесты форм — см. «Планы».

---

## Деплой

Production-схема: **reverse proxy (nginx/caddy) → приложение в Docker → Postgres в Docker**.

```bash
# На сервере
git clone <repo> && cd PoemOrder
cp .env.example .env   # заполнить реальными значениями
docker compose up -d --build
```

Замечания:

- Приложение слушает `127.0.0.1:8081` (так прописано в compose) — наружу его открывает только reverse proxy с TLS.
- Proxy должен передавать `X-Forwarded-For` и `X-Forwarded-Proto` — они учитываются и приложением, и rate-limiter'ом.
- Статику (`/css/**`, favicon) можно кэшировать на proxy.
- Бэкап БД: `docker exec poems_db pg_dump -U poems poems > backup.sql`.
- Образ собирается multi-stage (Maven → JRE), процесс работает **не под root**, память ограничена `MaxRAMPercentage=75`.

---

## Известные ограничения и планы

- **Rate limit и кэш в памяти процесса** — при нескольких инстансах нужен общий стор (Redis).
- **In-memory админы** — смена пароля требует рестарта; при росте — таблица пользователей в БД.
- **Публичная страница отзывов** выводит все одобренные без пагинации — при сотнях отзывов стоит ограничить.
- **Тесты** — минимальные; план: Testcontainers + MockMvc на формы и безопасность.
- Кандидаты на будущее: загрузка изображений к работам, sitemap/SEO-мета, страница статистики в админке.
