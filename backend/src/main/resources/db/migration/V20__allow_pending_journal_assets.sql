-- V20__allow_pending_journal_assets.sql
-- Ho tro upload anh nhap (PENDING_ATTACH), thu tu anh (position), va anh bia (is_cover) cho Journal Image Assets.

-- 1. Cho phep journal_entry_id nhan NULL khi anh o trang thai nhap
ALTER TABLE journal_assets ALTER COLUMN journal_entry_id DROP NOT NULL;

-- 2. Bo sung thu tu anh trong bai va co danh dau anh bia
ALTER TABLE journal_assets ADD COLUMN position INTEGER NOT NULL DEFAULT 0;
ALTER TABLE journal_assets ADD COLUMN is_cover BOOLEAN NOT NULL DEFAULT false;

-- 3. Cap nhat check constraint trang thai
ALTER TABLE journal_assets DROP CONSTRAINT IF EXISTS journal_assets_status_check;
ALTER TABLE journal_assets ADD CONSTRAINT journal_assets_status_check
    CHECK (status IN ('PENDING_ATTACH', 'PENDING_SCAN', 'READY', 'REJECTED', 'DELETED'));

-- 4. Bo sung index toi uu hoa truy van anh bia va don dep anh mo coi
CREATE INDEX idx_journal_assets_entry_cover ON journal_assets(journal_entry_id, status, is_cover, position);
CREATE INDEX idx_journal_assets_orphan_cleanup ON journal_assets(status, created_at) WHERE status = 'PENDING_ATTACH';
CREATE INDEX idx_journal_assets_user_sha256 ON journal_assets(user_id, sha256);

