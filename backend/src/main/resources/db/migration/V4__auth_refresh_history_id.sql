ALTER TABLE auth_refresh_history ADD COLUMN id UUID;

-- Existing rows predate application-assigned UUIDv7 IDs.
UPDATE auth_refresh_history SET id = gen_random_uuid();

ALTER TABLE auth_refresh_history ALTER COLUMN id SET NOT NULL;
ALTER TABLE auth_refresh_history ADD CONSTRAINT uq_auth_refresh_history_token_hash UNIQUE (token_hash);
ALTER TABLE auth_refresh_history DROP CONSTRAINT auth_refresh_history_pkey;
ALTER TABLE auth_refresh_history ADD CONSTRAINT auth_refresh_history_pkey PRIMARY KEY (id);
