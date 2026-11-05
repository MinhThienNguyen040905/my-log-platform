-- V19__oauth_google_support.sql
-- Hỗ trợ xác thực qua Google OAuth và tài khoản federated

-- 1. Xóa ràng buộc check auth_provider cũ một cách an toàn và tự động
DO $$
DECLARE
    c_name text;
BEGIN
    SELECT conname INTO c_name
    FROM pg_constraint
    WHERE conrelid = 'users'::regclass
      AND contype = 'c'
      AND pg_get_constraintdef(oid) LIKE '%auth_provider%';
      
    IF c_name IS NOT NULL THEN
        EXECUTE 'ALTER TABLE users DROP CONSTRAINT ' || quote_ident(c_name);
    END IF;
END $$;

-- 2. Thêm ràng buộc auth_provider mới: Hỗ trợ 'LOCAL' và 'GOOGLE'
ALTER TABLE users ADD CONSTRAINT users_auth_provider_check 
    CHECK (auth_provider IN ('LOCAL', 'GOOGLE'));

-- 3. Cho phép password_hash mang giá trị NULL đối với user Google
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- 4. Ràng buộc toàn vẹn: Tài khoản LOCAL bắt buộc phải có password_hash
ALTER TABLE users ADD CONSTRAINT users_password_provider_check
    CHECK (auth_provider <> 'LOCAL' OR password_hash IS NOT NULL);

-- 5. Tạo bảng liên kết danh tính OAuth (Federated Identities)
CREATE TABLE user_oauth_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL CHECK (provider IN ('GOOGLE')),
    provider_user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_user_oauth_provider_uid UNIQUE (provider, provider_user_id),
    CONSTRAINT uq_user_oauth_user_provider UNIQUE (user_id, provider)
);

-- 6. Bảo vệ an toàn dữ liệu theo ADR-0008 (Row Level Security & Revoke Grants)
REVOKE ALL PRIVILEGES ON TABLE user_oauth_identities FROM PUBLIC;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE ALL PRIVILEGES ON TABLE user_oauth_identities FROM anon;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL PRIVILEGES ON TABLE user_oauth_identities FROM authenticated;
    END IF;
END $$;
ALTER TABLE user_oauth_identities ENABLE ROW LEVEL SECURITY;

