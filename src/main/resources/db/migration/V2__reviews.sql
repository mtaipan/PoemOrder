create table if not exists reviews (
  id bigserial primary key,
  name varchar(50) not null,
  text varchar(2000) not null,
  telegram_username varchar(32),
  telegram_public boolean not null default false,
  status varchar(16) not null,
  created_at timestamptz not null
);

create index if not exists idx_reviews_status_created_at on reviews(status, created_at);
