-- ============ poem ============
create table if not exists poem (
    id           bigserial primary key,
    title        varchar(120) not null,
    excerpt      varchar(400),
    body         text not null,
    status       varchar(20) not null,
    created_at   timestamp not null default now(),
    updated_at   timestamp not null default now()
);

create index if not exists idx_poem_status_updated
    on poem (status, updated_at desc);
