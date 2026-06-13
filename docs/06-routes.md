# Маршруты

Полный справочник HTTP-маршрутов. Контроллеры: `web/public_/` и `web/admin/`.

## Публичные

| Метод | Путь | Контроллер | Поведение |
|---|---|---|---|
| GET | `/` | `PublicPageController.index` | Главная: 3 последних опубликованных стиха (`featuredWorks`), 3 последних одобренных отзыва (`featuredReviews`) |
| GET | `/portfolio` | `PublicPageController.portfolio` | Все опубликованные стихи, новые сверху |
| GET | `/portfolio/{id}` | `PublicPageController.portfolioItem` | Полный текст стиха; несуществующий id → ошибка (страница 500/404) |
| GET | `/pricing` | `PublicPageController.pricing` | Тексты блоков из настроек |
| GET | `/reviews` | `ReviewPublicController.reviews` | Одобренные отзывы + форма |
| POST | `/reviews` | `ReviewPublicController.submitReview` | Создаёт отзыв `PENDING`; honeypot и rate limit; ошибки валидации рендерятся на странице |
| GET | `/contacts` | `PublicPageController.contacts` | Карточки `contact_link` (enabled, по sort_order) |
| GET | `/order` | `OrderController.orderPage` | Форма заявки |
| POST | `/order` | `OrderController.submitOrder` | Создаёт заявку `NEW`; honeypot, rate limit, валидация через flash+redirect |
| GET | `/actuator/health` | actuator | `{"status":"UP"}` без деталей |
| GET | `/actuator/info` | actuator | пусто (зарезервировано) |

Модель каждого публичного запроса дополняется `GlobalModelAttributes` (`@ControllerAdvice` на пакет `public_`): `settings` (объект `SiteSettings`), `contactLinks`, `siteName`, `authorName`, `footerNote`.

### Параметры форм

**POST /order** (`OrderForm`):

| Поле | Валидация |
|---|---|
| `name` | обязательное, ≤80 |
| `phone` | обязательное, ≤30 |
| `social` | необязательное, ≤80 |
| `description` | обязательное, ≤4000 |
| `website` | honeypot — должен остаться пустым |
| `_csrf` | обязательный токен |

**POST /reviews** (`ReviewForm`):

| Поле | Валидация |
|---|---|
| `name` | обязательное, ≤50 |
| `text` | обязательное, ≤2000 |
| `telegramUsername` | необязательное, ≤32; ведущий `@` срезается |
| `telegramPublic` | чекбокс |
| `website` | honeypot |
| `_csrf` | обязательный токен |

### Коды ответов публичных POST

| Ситуация | Ответ |
|---|---|
| Успех | `302` на ту же страницу + flash-сообщение |
| Honeypot заполнен | `302` «успех» (бота не палим), записи нет |
| Ошибка валидации | `302` назад с ошибками (order) / `200` страница с ошибками (reviews) |
| Нет/битый CSRF | `403` |
| Превышен rate limit | `429` + текст «Слишком много запросов…» |

## Админские (роль ADMIN)

### Аутентификация

| Метод | Путь | Поведение |
|---|---|---|
| GET | `/admin/login` | страница логина |
| POST | `/admin/login` | обрабатывает Spring Security; успех → `/admin/dashboard`, ошибка → `?error=1` |
| POST | `/admin/logout` | выход → `/admin/login?logout=1` |
| GET | `/admin` | редирект на `/admin/dashboard` |

### Стихи — `AdminPoemController`

| Метод | Путь | Поведение |
|---|---|---|
| GET | `/admin/poems?page=N` | список, 20/стр., по `updatedAt` desc |
| GET | `/admin/poems/new` | форма создания (статус по умолчанию DRAFT) |
| POST | `/admin/poems/new` | создать; ошибки валидации — на форме; успех → redirect на edit с `?ok` |
| GET | `/admin/poems/{id}/edit` | форма редактирования |
| POST | `/admin/poems/{id}/edit` | сохранить |
| POST | `/admin/poems/{id}/delete` | удалить |

### Заказы — `AdminOrderController`

| Метод | Путь | Поведение |
|---|---|---|
| GET | `/admin/orders?page=N` | список, 20/стр., новые сверху |
| GET | `/admin/orders/{id}` | карточка заказа + выбор статуса |
| POST | `/admin/orders/{id}/status` | смена статуса (`status=NEW\|IN_PROGRESS\|DONE\|CANCELED`) |

### Отзывы — `AdminReviewController`

| Метод | Путь | Поведение |
|---|---|---|
| GET | `/admin/reviews?page=N` | очередь модерации (вся) + архив одобренных (20/стр.) |
| POST | `/admin/reviews/{id}/approve` | `PENDING → APPROVED` |
| POST | `/admin/reviews/{id}/hide-contact` | обнуляет telegram-контакт отзыва |
| POST | `/admin/reviews/{id}/delete` | удалить |

### Контакты — `AdminContactLinkController`

| Метод | Путь | Поведение |
|---|---|---|
| GET | `/admin/contacts` | список всех карточек (включая выключенные) |
| POST | `/admin/contacts/save` | upsert (`id` пустой = создание); невалидная схема `href` → redirect `?error` |
| POST | `/admin/contacts/{id}/delete` | удалить |

### Настройки — `AdminSettingsController`

| Метод | Путь | Поведение |
|---|---|---|
| GET | `/admin/settings` | форма настроек |
| POST | `/admin/settings` | сохранить; пустые строки → NULL; кэш сбрасывается |

### Dashboard

| Метод | Путь | Поведение |
|---|---|---|
| GET | `/admin/dashboard` | статическая стартовая страница |

## Всё остальное

`anyRequest().denyAll()` — аноним получает редирект на `/admin/login`, аутентифицированный — `403`. Новые маршруты надо явно разрешать в `SecurityConfig` (см. [05-security.md](05-security.md)).
