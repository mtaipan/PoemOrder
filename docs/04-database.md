# База данных

PostgreSQL 16. Схемой владеет Flyway: `spring.jpa.hibernate.ddl-auto=none`, Hibernate только читает/пишет данные.

## Таблицы

### `poem` — стихи (портфолио)

| Колонка | Тип | Ограничения | Описание |
|---|---|---|---|
| `id` | bigserial | PK | |
| `title` | varchar(120) | not null | название |
| `excerpt` | varchar(400) | nullable | отрывок для карточки; если пуст — на витрине показывается обрезка body до 300 символов |
| `body` | text | not null | полный текст |
| `status` | varchar(20) | not null | `DRAFT` / `PUBLISHED` (enum `PoemStatus` как строка) |
| `created_at` | timestamptz | not null | ставится в `@PrePersist` |
| `updated_at` | timestamptz | not null | обновляется в `@PreUpdate`; по нему сортируются витрина и админка |

Индекс: `idx_poem_status_updated (status, updated_at desc)` — покрывает выборку «опубликованные по свежести».

### `orders` — заявки

| Колонка | Тип | Ограничения | Описание |
|---|---|---|---|
| `id` | bigserial | PK | |
| `name` | varchar(80) | not null | имя клиента |
| `phone` | varchar(30) | not null | телефон (строкой, без нормализации) |
| `social` | varchar(80) | nullable | ник в соцсети/мессенджере |
| `description` | varchar(4000) | not null | описание заказа |
| `status` | varchar(20) | not null | `NEW` / `IN_PROGRESS` / `DONE` / `CANCELED` |
| `created_at` | timestamptz | not null, default now() | |

Индексы: `idx_orders_created_at (created_at desc)`, `idx_orders_status (status)`.

Имя таблицы — `orders`, потому что `order` в SQL зарезервировано.

### `reviews` — отзывы

| Колонка | Тип | Ограничения | Описание |
|---|---|---|---|
| `id` | bigserial | PK | |
| `name` | varchar(50) | not null | имя автора отзыва |
| `text` | varchar(2000) | not null | текст |
| `telegram_username` | varchar(32) | nullable | без `@` (сервис срезает при сохранении) |
| `telegram_public` | boolean | not null, default false | разрешён ли показ контакта на сайте |
| `status` | varchar(16) | not null | `PENDING` / `APPROVED` |
| `created_at` | timestamptz | not null | |

Индекс: `idx_reviews_status_created_at (status, created_at)`.

Жизненный цикл: публичная форма создаёт `PENDING` → админ либо одобряет (`APPROVED`), либо удаляет. Отдельное действие «скрыть контакт» обнуляет `telegram_username` и сбрасывает `telegram_public`.

### `site_settings` — настройки сайта (singleton)

Ровно одна строка с `id = 1`, создаётся миграцией `V3`. Приложение бросает `IllegalStateException`, если строки нет.

| Колонка | Тип | Описание |
|---|---|---|
| `id` | smallint PK | всегда 1 |
| `hero_title` | varchar(120) not null | заголовок главной; используется и как имя сайта в шапке/футере |
| `hero_subtitle` | varchar(500) not null | подзаголовок главной |
| `portfolio_title` | varchar(120) | заголовок страницы портфолио (null → дефолт в шаблоне) |
| `portfolio_subtitle` | text | подзаголовок портфолио |
| `pricing_title` | varchar(160) | заголовок страницы цен |
| `pricing_payment` | text | блок «Оплата» (многострочный) |
| `pricing_delivery` | text | блок «Доставка» |
| `pricing_refund` | text | блок «Возврат» |
| `telegram`, `phone`, `email`, `social` | varchar | легаси-контакты; публичная страница контактов теперь использует `contact_link` |
| `updated_at` | timestamptz not null | |

Пустые строки из формы админки нормализуются в `NULL` (`SiteSettingsService.emptyToNull`), чтобы шаблоны могли показывать дефолты через простую проверку на null.

### `contact_link` — карточки контактов

| Колонка | Тип | Описание |
|---|---|---|
| `id` | bigserial PK | |
| `label` | varchar(120) not null | подпись («Telegram», «Почта»…) |
| `value` | varchar(300) not null | отображаемое значение («@poet») |
| `href` | varchar(500) | ссылка; **только** `http://`, `https://`, `mailto:`, `tel:` (валидация в `ContactLinkService`); null → просто текст |
| `sort_order` | int not null default 0 | порядок на странице |
| `enabled` | boolean not null default true | выключенные не показываются публично |

### `flyway_schema_history`

Служебная таблица Flyway. Руками не трогать.

## Миграции

Расположение: `src/main/resources/db/migration`. Применяются автоматически при старте.

| Версия | Что делает |
|---|---|
| `V1__init.sql` | историческая bootstrap-миграция (создавала `_boot_ok`; таблица удалена в V10) |
| `V2__reviews.sql` | таблица отзывов + индекс |
| `V3__site_settings_and_portfolio.sql` | `site_settings` (+дефолтная строка id=1) и `portfolio_item` (удалена в V10) |
| `V4__orders.sql` | таблица заявок + индексы |
| `V5__site_settings_email.sql` | + колонка `email` |
| `V6__site_settings_pricing.sql` | + колонки ценовых блоков |
| `V7__poems.sql` | таблица `poem` + индекс |
| `V8__contact_links.sql` | таблица `contact_link` + индекс |
| `V9__site_settings_portfolio_text.sql` | + тексты страницы портфолио |
| `V10__cleanup_schema.sql` | удаление неиспользуемых `portfolio_item` и `_boot_ok`; приведение `poem.*_at` и `site_settings.updated_at` к `timestamptz` |

### Правила

1. **Применённые миграции не редактируются.** Любое изменение — новый файл `V<n+1>__описание.sql`. Flyway хранит чексуммы и упадёт при расхождении.
2. **Время — только `timestamptz`.** После V10 вся схема единообразна; новые колонки времени создавать так же.
3. **Никаких `IF NOT EXISTS` в новых миграциях.** Исторические файлы им злоупотребляют (V1–V9 писались до этого правила); смысл Flyway — детерминированная схема, `IF NOT EXISTS` маскирует дрейф.
4. Данные-инициализация (как строка `site_settings`) — тоже в миграциях, с `on conflict do nothing`.
5. После переименования файла миграции локально — чисти `target/classes/db` (или `mvnw clean`), иначе Flyway увидит обе копии и упадёт с «Found more than one migration with version N».

## Подключение и пул

- HikariCP: максимум 10 соединений (`DB_POOL_SIZE`), минимум 2 idle, connection timeout 30 с.
- Для текущей нагрузки (личный сайт) этого с запасом; Postgres по умолчанию принимает 100.

## Бэкапы

```bash
# дамп
docker exec poems_db pg_dump -U poems poems > backup_$(date +%F).sql
# восстановление
cat backup_2026-06-12.sql | docker exec -i poems_db psql -U poems poems
```

Данные Postgres живут в named volume `pgdata` — `docker compose down` их не удаляет (удаляет `down -v`).
