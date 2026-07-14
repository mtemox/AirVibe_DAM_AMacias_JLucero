-- =============================================================
-- update_schema_feature3.sql
-- Feature 3: "El Handshake (Conexión y Networking P2P)"
--
-- Crea la tabla `public.handshake_requests` que persiste las
-- solicitudes de conexión P2P (entrantes y salientes) y su
-- estado. Se sincroniza con la tabla `saved_contacts` cuando
-- una solicitud se acepta.
--
-- Es idempotente: re-ejecutable sin errores.
-- =============================================================

CREATE TABLE IF NOT EXISTS public.handshake_requests (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id            UUID         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    handshake_id        TEXT         NOT NULL,
    peer_node_id        TEXT         NOT NULL,
    peer_display_name   TEXT         NOT NULL DEFAULT '',
    peer_headline       TEXT         NOT NULL DEFAULT '',
    peer_status         TEXT         NOT NULL DEFAULT '',
    peer_presence       TEXT         NOT NULL DEFAULT 'Online',
    peer_tags           TEXT[]       NOT NULL DEFAULT '{}',
    handshake_key       TEXT         NOT NULL,
    status              TEXT         NOT NULL DEFAULT 'Pending',
    direction           TEXT         NOT NULL DEFAULT 'Incoming',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    responded_at        TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT handshake_requests_owner_peer_unique
        UNIQUE (owner_id, handshake_id),
    CONSTRAINT handshake_requests_status_check
        CHECK (status IN ('Pending', 'Accepted', 'Rejected', 'Expired', 'Cancelled')),
    CONSTRAINT handshake_requests_direction_check
        CHECK (direction IN ('Incoming', 'Outgoing'))
);

CREATE INDEX IF NOT EXISTS idx_handshake_requests_owner
    ON public.handshake_requests(owner_id);
CREATE INDEX IF NOT EXISTS idx_handshake_requests_owner_status
    ON public.handshake_requests(owner_id, status);
CREATE INDEX IF NOT EXISTS idx_handshake_requests_peer
    ON public.handshake_requests(peer_node_id);

DROP TRIGGER IF EXISTS trg_handshake_requests_updated_at ON public.handshake_requests;
CREATE TRIGGER trg_handshake_requests_updated_at
    BEFORE UPDATE ON public.handshake_requests
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- ===========================================================
-- ROW LEVEL SECURITY
-- ===========================================================
ALTER TABLE public.handshake_requests ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "handshake_requests_select_own" ON public.handshake_requests;
DROP POLICY IF EXISTS "handshake_requests_insert_own" ON public.handshake_requests;
DROP POLICY IF EXISTS "handshake_requests_update_own" ON public.handshake_requests;
DROP POLICY IF EXISTS "handshake_requests_delete_own" ON public.handshake_requests;

CREATE POLICY "handshake_requests_select_own"
    ON public.handshake_requests FOR SELECT TO authenticated
    USING (owner_id = auth.uid());

CREATE POLICY "handshake_requests_insert_own"
    ON public.handshake_requests FOR INSERT TO authenticated
    WITH CHECK (owner_id = auth.uid());

CREATE POLICY "handshake_requests_update_own"
    ON public.handshake_requests FOR UPDATE TO authenticated
    USING (owner_id = auth.uid()) WITH CHECK (owner_id = auth.uid());

CREATE POLICY "handshake_requests_delete_own"
    ON public.handshake_requests FOR DELETE TO authenticated
    USING (owner_id = auth.uid());

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
    WHERE table_schema = 'public' AND table_name = 'handshake_requests';

    SELECT COUNT(*) INTO policy_count
    FROM pg_policies
    WHERE schemaname = 'public' AND tablename = 'handshake_requests';

    SELECT COUNT(*) INTO index_count
    FROM pg_indexes
    WHERE schemaname = 'public' AND tablename = 'handshake_requests';

    RAISE NOTICE '✅ Feature3 OK. handshake_requests: tabla=%, policies=%, indices=%',
        table_count, policy_count, index_count;
END $$;
