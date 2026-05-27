-- Add optional `icon` column to platform_menu.
--
-- The admin UI renders menu items with PrimeIcons-style icon classes
-- (e.g. "pi-users", "pi-cog"); store the raw token here so the UI can
-- compose the full class at render time. Stays nullable -- menu items
-- without an icon render as text-only.

ALTER TABLE platform_menu
    ADD COLUMN icon VARCHAR(64);
