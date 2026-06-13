create table if not exists orders (
  id bigserial primary key,
  name varchar(80) not null,
  phone varchar(30) not null,
  social varchar(80),
  description varchar(4000) not null,
  status varchar(20) not null,
  created_at timestamptz not null default now()
);

create index if not exists idx_orders_created_at on orders(created_at desc);
create index if not exists idx_orders_status on orders(status);
