create table if not exists contact_link (
    id          bigserial primary key,
    label       varchar(120) not null,
    value       varchar(300) not null,
    href        varchar(500),
    sort_order  int not null default 0,
    enabled     boolean not null default true
);

create index if not exists idx_contact_link_sort
    on contact_link (sort_order);
