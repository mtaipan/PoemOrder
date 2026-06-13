ALTER TABLE site_settings
  ADD COLUMN IF NOT EXISTS pricing_title varchar(160),
  ADD COLUMN IF NOT EXISTS pricing_payment text,
  ADD COLUMN IF NOT EXISTS pricing_delivery text,
  ADD COLUMN IF NOT EXISTS pricing_refund text;
