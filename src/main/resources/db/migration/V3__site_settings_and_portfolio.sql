-- ============ site_settings (одна строка) ============
create table if not exists site_settings (
    id                 smallint primary key,
    hero_title         varchar(120) not null,
    hero_subtitle      varchar(500) not null,
    telegram           varchar(80),
    phone              varchar(40),
    social             varchar(120),
    updated_at         timestamp not null default now()
);

-- создаём запись по умолчанию
insert into site_settings (id, hero_title, hero_subtitle, telegram, phone, social)
values (1,
        'Стихи на заказ',
        'Пишу авторские стихи: поздравления, признания, свадьбы, юбилеи. По твоей истории и под нужный стиль.',
        null, null, null)
on conflict (id) do nothing;

-- ============ portfolio_item ============
create table if not exists portfolio_item (
    id           bigserial primary key,
    title        varchar(120) not null,
    excerpt      varchar(400),
    body         text not null,
    kind         varchar(20) not null,
    status       varchar(20) not null,
    created_at   timestamp not null default now(),
    updated_at   timestamp not null default now()
);

create index if not exists idx_portfolio_item_status_updated
    on portfolio_item (status, updated_at desc);
