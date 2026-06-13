-- portfolio_item никогда не отображался на сайте (публичное портфолио читает таблицу poem),
-- _boot_ok — технологическая таблица из V1, не нужна.
drop table if exists portfolio_item;
drop table if exists _boot_ok;

-- приводим оставшиеся timestamp-колонки к timestamptz, как в reviews/orders
-- (приложение всегда писало UTC: hibernate.jdbc.time_zone=UTC)
alter table poem
    alter column created_at type timestamptz using created_at at time zone 'UTC',
    alter column updated_at type timestamptz using updated_at at time zone 'UTC';

alter table site_settings
    alter column updated_at type timestamptz using updated_at at time zone 'UTC';
