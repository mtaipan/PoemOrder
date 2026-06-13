ALTER TABLE site_settings
ADD COLUMN IF NOT EXISTS email varchar(120);
