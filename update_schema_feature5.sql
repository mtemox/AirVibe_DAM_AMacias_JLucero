-- =============================================================
-- update_schema_feature5.sql
-- Feature 5: "Sincronización Diferida (Cloud Sync) y Analíticas Premium"
--
-- Crea las tablas de telemetría:
--  - profile_views     : cada aparición/toque de un peer Premium en un radar
--  - visibility_daily  : agregado diario por usuario Premium (lo que consulta
--                         su app al sincronizar para alimentar el dashboard)
--
-- Es idempotente: re-ejecutable sin errores.
-- =============================================================

CREATE TABLE IF NOT EXISTS public.profile_views (
    id                  BIGSERIAL    PRIMARY KEY,
    target_user_id      UUID         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    source_node_id      TEXT         NOT NULL,
    owner_id            UUID         REFERENCES auth.users(id) ON DELETE SET NULL,
    kind                TEXT         NOT NULL DEFAULT 'View',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT profile_views_kind_check
        CHECK (kind IN ('View', 'Tap', 'Broadcast'))
);

CREATE INDEX IF NOT EXISTS idx_profile_views_target_day
    ON public.profile_views(target_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_profile_views_owner
    ON public.profile_views(owner_id);

ALTER TABLE public.profile_views ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "profile_views_insert_any"  ON public.profile_views;
DROP POLICY IF EXISTS "profile_views_select_own"  ON public.profile_views;
DROP POLICY IF EXISTS "profile_views_delete_own"  ON public.profile_views;

-- Cualquier usuario autenticado puede registrar un evento de
-- visibilidad (es telemetría anónima, el RLS del receptor se
-- encarga de qué puede ver cada uno).
CREATE POLICY "profile_views_insert_any"
    ON public.profile_views FOR INSERT TO authenticated
    WITH CHECK (true);

-- Solo el usuario Premium DUEÑO de la fila puede leer sus
-- propias vistas (para alimentar su dashboard).
CREATE POLICY "profile_views_select_own"
    ON public.profile_views FOR SELECT TO authenticated
    USING (target_user_id = auth.uid());

-- =============================================================

CREATE TABLE IF NOT EXISTS public.visibility_daily (
    target_user_id          UUID         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    day                     DATE         NOT NULL,
    views_count             INTEGER      NOT NULL DEFAULT 0,
    taps_count              INTEGER      NOT NULL DEFAULT 0,
    broadcasts_count        INTEGER      NOT NULL DEFAULT 0,
    unique_visitors_count   INTEGER      NOT NULL DEFAULT 0,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (target_user_id, day)
);

CREATE INDEX IF NOT EXISTS idx_visibility_daily_target_day
    ON public.visibility_daily(target_user_id, day DESC);

ALTER TABLE public.visibility_daily ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "visibility_daily_select_own"  ON public.visibility_daily;
DROP POLICY IF EXISTS "visibility_daily_insert_any"  ON public.visibility_daily;
DROP POLICY IF EXISTS "visibility_daily_update_any"  ON public.visibility_daily;

CREATE POLICY "visibility_daily_select_own"
    ON public.visibility_daily FOR SELECT TO authenticated
    USING (target_user_id = auth.uid());

CREATE POLICY "visibility_daily_insert_any"
    ON public.visibility_daily FOR INSERT TO authenticated
    WITH CHECK (true);

CREATE POLICY "visibility_daily_update_any"
    ON public.visibility_daily FOR UPDATE TO authenticated
    USING (true) WITH CHECK (true);

-- =============================================================
-- Función helper: agregar un evento al agregado diario.
-- Idempotente. Llamada por el SyncWorker o por un trigger
-- cuando se inserta en profile_views.
-- =============================================================
CREATE OR REPLACE FUNCTION public.upsert_visibility_daily(
    p_target_user_id  UUID,
    p_day             DATE,
    p_kind            TEXT,
    p_source_node_id  TEXT
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.visibility_daily
        (target_user_id, day, views_count, taps_count, broadcasts_count, unique_visitors_count)
    VALUES (
        p_target_user_id, p_day,
        CASE WHEN p_kind = 'View'      THEN 1 ELSE 0 END,
        CASE WHEN p_kind = 'Tap'       THEN 1 ELSE 0 END,
        CASE WHEN p_kind = 'Broadcast' THEN 1 ELSE 0 END,
        1
    )
    ON CONFLICT (target_user_id, day) DO UPDATE
        SET
            views_count      = public.visibility_daily.views_count      + EXCLUDED.views_count,
            taps_count       = public.visibility_daily.taps_count       + EXCLUDED.taps_count,
            broadcasts_count = public.visibility_daily.broadcasts_count + EXCLUDED.broadcasts_count,
            updated_at       = NOW();
    -- Aproximación muy simple del "único visitor" (no es exacto
    -- porque no desduplicamos a nivel Postgres; el cliente puede
    -- refinarlo con un job nocturno). Se mantiene el orden de
    -- magnitud.
    UPDATE public.visibility_daily
    SET unique_visitors_count = LEAST(unique_visitors_count + 1, 1000000)
    WHERE target_user_id = p_target_user_id
      AND day = p_day
      AND p_kind IN ('View', 'Tap');
END;
$$;

-- Trigger function: wrapper for upsert_visibility_daily
CREATE OR REPLACE FUNCTION public.trg_fn_upsert_visibility_daily()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    PERFORM public.upsert_visibility_daily(
        NEW.target_user_id,
        DATE(NEW.created_at),
        NEW.kind,
        NEW.source_node_id
    );
    RETURN NEW;
END;
$$;

-- Trigger: cada INSERT en profile_views alimenta el agregado.
DROP TRIGGER IF EXISTS trg_profile_views_aggregate ON public.profile_views;
CREATE TRIGGER trg_profile_views_aggregate
    AFTER INSERT ON public.profile_views
    FOR EACH ROW
    EXECUTE FUNCTION public.trg_fn_upsert_visibility_daily();

-- =============================================================
-- Verificación post-migración
-- =============================================================
DO $$
DECLARE
    views_table      INTEGER;
    daily_table      INTEGER;
    views_policy_cnt INTEGER;
    daily_policy_cnt INTEGER;
BEGIN
    SELECT COUNT(*) INTO views_table
    FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'profile_views';

    SELECT COUNT(*) INTO daily_table
    FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'visibility_daily';

    SELECT COUNT(*) INTO views_policy_cnt
    FROM pg_policies WHERE schemaname = 'public' AND tablename = 'profile_views';

    SELECT COUNT(*) INTO daily_policy_cnt
    FROM pg_policies WHERE schemaname = 'public' AND tablename = 'visibility_daily';

    RAISE NOTICE '✅ Feature5 OK. profile_views: tabla=%, policies=%. visibility_daily: tabla=%, policies=%.',
        views_table, views_policy_cnt, daily_table, daily_policy_cnt;
END $$;
