# Архитектура

## Общая схема

Классическое слоистое server-side rendered приложение. Один процесс, одна БД.

```
Браузер
   │ HTTP
   ▼
┌─────────────────────────────────────────────────┐
│ Servlet-фильтры                                  │
│  ForwardedHeaderFilter (X-Forwarded-*)           │
│  RateLimitFilter (POST /order, /reviews)         │
│  Spring Security chain (auth, CSRF, headers)     │
└────────────────────┬────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────┐
│ Контроллеры  web/public_  web/admin              │
│  биндинг форм в DTO + Bean Validation            │
│  GlobalModelAttributes (настройки в модель)      │
└────────────────────┬────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────┐
│ Сервисы  service/   @Transactional               │
│  бизнес-логика, маппинг DTO→entity               │
│  кэш (siteSettings, contactLinks)                │
└──────┬──────────────────────────┬───────────────┘
       ▼                          ▼
┌──────────────┐        ┌──────────────────────┐
│ Репозитории  │        │ TelegramNotifier      │
│ Spring Data  │        │ best-effort, таймауты │
│ JPA  repo/   │        │ 3s connect / 5s read  │
└──────┬───────┘        └──────────────────────┘
       ▼
  PostgreSQL  (схемой владеет Flyway)
```

## Пакеты

```
com.poemorder.app
├── config/         SecurityConfig, CacheConfig — инфраструктурная конфигурация
├── domain/         JPA-сущности, сгруппированы по агрегатам (order, poem, review, settings)
├── dto/            формы публичной части (OrderForm, ReviewForm) с Bean Validation
├── repo/           интерфейсы Spring Data JPA
├── service/        транзакционная бизнес-логика
├── integration/    внешние системы (telegram/)
└── web/
    ├── RateLimitFilter
    ├── admin/      контроллеры /admin/**
    └── public_/    публичные контроллеры + GlobalModelAttributes
```

Правило зависимостей: `web → service → repo/integration`. Контроллеры не трогают репозитории напрямую (исключений нет), сущности не знают о web-слое.

## Поток запроса на примерах

### Публичная страница: `GET /pricing`

1. `RateLimitFilter.shouldNotFilter` → true (не POST) — пропускает.
2. Security: `/pricing` в permit-list — пропускает анонима.
3. `PublicPageController.pricing()` кладёт `activePage` в модель.
4. `GlobalModelAttributes` (для всех контроллеров пакета `public_`) добавляет `settings` и `contactLinks` — **из кэша**, БД не трогается.
5. Thymeleaf рендерит `public/pricing.html` внутри layout `layouts/public.html`.

### Отправка заявки: `POST /order`

1. `RateLimitFilter`: определяет IP клиента (первый адрес из `X-Forwarded-For`, иначе remote addr), инкрементирует счётчик окна. Превышение → `429` сразу, дальше запрос не идёт.
2. Security: путь разрешён, но проверяется **CSRF-токен** — без него `403`.
3. `OrderController.submitOrder`: биндинг в `OrderForm`, проверка honeypot-поля `website` (заполнено → имитируем успех и выходим), затем Bean Validation.
4. Ошибки валидации → flash + redirect обратно на форму (PRG); ошибки переживают редирект через `RedirectAttributes`.
5. `OrderService.createFromForm`: маппинг в сущность `Order` (trim полей), `status=NEW`, save.
6. Тот же метод вызывает `TelegramNotifier.send(...)` — при недоступности Telegram пишется warning в лог, заявка всё равно сохранена.
7. Redirect на `/order` с flash-сообщением об успехе.

### Админское действие: `POST /admin/reviews/{id}/approve`

1. Security: требуется роль `ADMIN` (сессия после form login) + CSRF.
2. `AdminReviewController.approve` → `ReviewService.approve(id)`: загрузка, смена статуса на `APPROVED` — изменение фиксируется dirty checking'ом при коммите транзакции.
3. Flash «Отзыв опубликован» + redirect на список (PRG).

## Ключевые решения и их причины

### Server-side rendering, без JS-фреймворков
Контента мало, интерактивность минимальна (формы и ссылки), SEO критично для витрины. SPA добавил бы сборку фронта, API-слой и дублирование валидации без выгоды.

### Schema-first: Flyway владеет схемой
`spring.jpa.hibernate.ddl-auto=none`. Любое изменение схемы — новый версионированный SQL-файл. Hibernate никогда не генерирует DDL, поэтому схема воспроизводима и диффится в git. Подробности в [04-database.md](04-database.md).

### `open-in-view=false`
Ленивая загрузка за пределами сервисного слоя запрещена. Все данные для шаблона собираются в `@Transactional`-методах сервисов. Это исключает N+1 в шаблонах и «LazyInitializationException в проде вечером в пятницу».

### DTO на входе, сущности — только внутри
Публичные формы (`OrderForm`, `ReviewForm`) — отдельные классы с Bean Validation. Это защита от mass assignment: клиент физически не может передать `id`, `status` или `createdAt`. В админке допускается биндинг в сущность (формы редактирования 1:1 с моделью), но системные поля принудительно затираются в сервисе (`PoemService.create` обнуляет `id` и `createdAt`).

### PRG (Post/Redirect/Get) везде
Каждый успешный POST заканчивается redirect'ом. F5 после отправки формы не создаёт дубликат. Сообщения и ошибки валидации передаются flash-атрибутами.

### Кэш справочных данных
`site_settings` (одна строка) и `contact_link` читаются на **каждом** публичном запросе через `GlobalModelAttributes`. Оба закэшированы in-memory (`ConcurrentMapCacheManager`, бин в `CacheConfig` — в Boot 4 cache-автоконфигурация в отдельном стартере, поэтому бин объявлен явно). Инвалидация — `@CacheEvict` в методах изменения. TTL нет: данные меняются только через эти же методы.

Важный нюанс: `SiteSettingsService.update()` читает сущность напрямую из репозитория, а не через собственный `get()` — самовызов обошёл бы кэш-прокси, но главное, нужна свежая managed-сущность, а не закэшированная отсоединённая копия.

### Модерация отзывов через статусы
Отзыв всегда создаётся `PENDING` и невидим публично. Никаких «опубликовать сразу с возможностью скрыть» — по умолчанию закрыто. Поле `telegram_public` отдельно управляет видимостью контакта даже у одобренного отзыва.

### Уведомления best-effort
Telegram-вызов синхронный, но с жёсткими таймаутами (3 с connect / 5 с read) и `try/catch` на всё. Недоступный Telegram замедлит сабмит максимум на 8 секунд, но не уронит его. Очередь/ретраи не оправданы: уведомление дублируется списком заказов в админке.

### Часовые пояса
Всё время — `Instant` в Java, `timestamptz` в Postgres, `hibernate.jdbc.time_zone=UTC`. Отображение в шаблонах форматируется без явной зоны (даты в админке носят информативный характер).

## Компромиссы, о которых стоит знать

- **In-memory всё** (кэш, rate limit, сессии, админы) — корректно ровно до второго инстанса приложения. Горизонтальное масштабирование потребует Redis (кэш+rate limit), JDBC session store и таблицу пользователей.
- **Синхронный Telegram** — до 8 с задержки сабмита при лежащем Telegram. Сознательная плата за отсутствие очереди.
- **`anyRequest().denyAll()`** — каждый новый публичный маршрут нужно явно прописать в `SecurityConfig`, иначе он будет «недоступен» (редирект на логин). Это намеренная цена за безопасный дефолт.
