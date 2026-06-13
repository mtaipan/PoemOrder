ALTER TABLE site_settings
    ADD COLUMN IF NOT EXISTS portfolio_title VARCHAR(120),
    ADD COLUMN IF NOT EXISTS portfolio_subtitle TEXT;
