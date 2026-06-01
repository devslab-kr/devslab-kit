ALTER TABLE platform_user_account
    ADD COLUMN IF NOT EXISTS must_change_password boolean NOT NULL DEFAULT false;
