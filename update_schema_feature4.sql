-- =============================================================
-- update_schema_feature4.sql
-- Feature 4: "Comunicación P2P (Chats y Salas de Proximidad)"
--
-- Crea la tabla `public.room_members` que mantiene la lista de
-- invitados/participantes de cada sala. Permite a un cliente
-- sincronizar la composición de una sala entre dispositivos y
-- persistir quién es Host vs Guest.
--
-- Es idempotente: re-ejecutable sin errores.
-- =============================================================

CREATE TABLE IF NOT EXISTS public.room_members (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    room_id         TEXT         NOT NULL,
    node_id         TEXT         NOT NULL,
    display_name    TEXT         NOT NULL DEFAULT '',
    role            TEXT         NOT NULL DEFAULT 'Guest',
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    joined_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_seen       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT room_members_owner_room_node_unique
        UNIQUE (owner_id, room_id, node_id),
    CONSTRAINT room_members_role_check
        CHECK (role IN ('Host', 'Guest'))
);

CREATE INDEX IF NOT EXISTS idx_room_members_owner
    ON public.room_members(owner_id);
CREATE INDEX IF NOT EXISTS idx_room_members_room
    ON public.room_members(room_id);
CREATE INDEX IF NOT EXISTS idx_room_members_active
    ON public.room_members(room_id) WHERE is_active = TRUE;

DROP TRIGGER IF EXISTS trg_room_members_updated_at ON public.room_members;
CREATE TRIGGER trg_room_members_updated_at
    BEFORE UPDATE ON public.room_members
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- ===========================================================
-- ROW LEVEL SECURITY
-- ===========================================================
ALTER TABLE public.room_members ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "room_members_select_own" ON public.room_members;
DROP POLICY IF EXISTS "room_members_insert_own" ON public.room_members;
DROP POLICY IF EXISTS "room_members_update_own" ON public.room_members;
DROP POLICY IF EXISTS "room_members_delete_own" ON public.room_members;

CREATE POLICY "room_members_select_own"
    ON public.room_members FOR SELECT TO authenticated
    USING (owner_id = auth.uid());

CREATE POLICY "room_members_insert_own"
    ON public.room_members FOR INSERT TO authenticated
    WITH CHECK (owner_id = auth.uid());

CREATE POLICY "room_members_update_own"
    ON public.room_members FOR UPDATE TO authenticated
    USING (owner_id = auth.uid()) WITH CHECK (owner_id = auth.uid());

CREATE POLICY "room_members_delete_own"
    ON public.room_members FOR DELETE TO authenticated
    USING (owner_id = auth.uid());

-- =============================================================
-- Función helper: registrar un miembro (idempotente).
-- Útil desde el cliente cuando se une o se reconecta.
-- =============================================================
CREATE OR REPLACE FUNCTION public.upsert_room_member(
    p_room_id     TEXT,
    p_node_id     TEXT,
    p_display_name TEXT,
    p_role        TEXT DEFAULT 'Guest'
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.room_members (owner_id, room_id, node_id, display_name, role, is_active, last_seen)
    VALUES (auth.uid(), p_room_id, p_node_id, p_display_name, p_role, TRUE, NOW())
    ON CONFLICT (owner_id, room_id, node_id) DO UPDATE
        SET display_name = EXCLUDED.display_name,
            is_active    = TRUE,
            last_seen    = NOW();
END;
$$;

-- =============================================================
-- Verificación post-migración
-- =============================================================
DO $$
DECLARE
    table_count    INTEGER;
    policy_count   INTEGER;
    index_count    INTEGER;
BEGIN
    SELECT COUNT(*) INTO table_count
    FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'room_members';

    SELECT COUNT(*) INTO policy_count
    FROM pg_policies
    WHERE schemaname = 'public' AND tablename = 'room_members';

    SELECT COUNT(*) INTO index_count
    FROM pg_indexes
    WHERE schemaname = 'public' AND tablename = 'room_members';

    RAISE NOTICE '✅ Feature4 OK. room_members: tabla=%, policies=%, indices=%',
        table_count, policy_count, index_count;
END $$;
