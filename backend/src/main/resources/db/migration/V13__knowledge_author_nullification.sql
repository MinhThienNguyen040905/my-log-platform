-- Preserve reviewed content while allowing user deletion to null FK attribution fields.
CREATE OR REPLACE FUNCTION guard_approved_knowledge() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' AND OLD.status IN ('APPROVED','ARCHIVED') THEN
        RAISE EXCEPTION 'approved knowledge version is immutable';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.status IN ('APPROVED','ARCHIVED') THEN
        IF NEW.item_id IS DISTINCT FROM OLD.item_id OR NEW.version IS DISTINCT FROM OLD.version
            OR NEW.title IS DISTINCT FROM OLD.title OR NEW.content IS DISTINCT FROM OLD.content
            OR NEW.content_sha256 IS DISTINCT FROM OLD.content_sha256
            OR NEW.chunk_strategy_version IS DISTINCT FROM OLD.chunk_strategy_version
            OR (NEW.created_by IS DISTINCT FROM OLD.created_by AND NEW.created_by IS NOT NULL)
            OR (NEW.reviewed_by IS DISTINCT FROM OLD.reviewed_by AND NEW.reviewed_by IS NOT NULL)
            OR (NEW.approved_by IS DISTINCT FROM OLD.approved_by AND NEW.approved_by IS NOT NULL)
            OR NEW.approved_at IS DISTINCT FROM OLD.approved_at
            OR NEW.created_at IS DISTINCT FROM OLD.created_at
            OR NEW.effective_from IS DISTINCT FROM OLD.effective_from
            OR NEW.effective_to IS DISTINCT FROM OLD.effective_to
            OR (OLD.status = 'ARCHIVED' AND NEW.status <> 'ARCHIVED')
            OR NEW.status NOT IN ('APPROVED','ARCHIVED') THEN
            RAISE EXCEPTION 'approved knowledge version is immutable';
        END IF;
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;
