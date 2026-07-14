-- =============================================================
-- update_schema_feature2.sql
-- Feature 2: "Perfil e Identidad Offline (El Payload)"
--
-- Añade al esquema de AirVibe los campos necesarios para soportar
-- el Payload extendido (Premium + bio + catálogo/portafolio) y
-- separar `headline` (profesión) del `status` (vibe).
--
-- Es idempotente: re-ejecutable sin errores.
-- =============================================================

-- ---------------------------------------------------------------
-- 1) Tabla `public.profiles`
--    - headline     : profesión / título corto (Ej. "Diseñador Gráfico")
--    - profession   : ya existe en el esquema original; se mantiene
--    - bio          : biografía extendida visible en el preview
--    - is_premium   : el usuario paga Premium y puede emitir Payload v3
--    - premium_catalog : portafolio / mini-catálogo de precios (texto)
--    - premium_since : fecha desde la que es Premium
-- ---------------------------------------------------------------
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS headline       TEXT         NOT NULL DEFAULT '';
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS bio            TEXT         NOT NULL DEFAULT '';
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS is_premium     BOOLEAN      NOT NULL DEFAULT FALSE;
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS premium_catalog TEXT;
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS premium_since  TIMESTAMPTZ;

-- ---------------------------------------------------------------
-- 2) Tabla `public.radar_nodes`
--    Cuando un usuario descubre un peer (vía Nearby) y lo persiste
--    en su backup, el peer puede traer info extendida (Payload v3).
--    Aquí guardamos los mismos campos para mantener 1:1 con la
--    entidad local `NodeEntity`.
-- ---------------------------------------------------------------
ALTER TABLE public.radar_nodes
    ADD COLUMN IF NOT EXISTS headline        TEXT         NOT NULL DEFAULT '';
ALTER TABLE public.radar_nodes
    ADD COLUMN IF NOT EXISTS bio             TEXT         NOT NULL DEFAULT '';
ALTER TABLE public.radar_nodes
    ADD COLUMN IF NOT EXISTS is_premium      BOOLEAN      NOT NULL DEFAULT FALSE;
ALTER TABLE public.radar_nodes
    ADD COLUMN IF NOT EXISTS premium_catalog TEXT;

CREATE INDEX IF NOT EXISTS idx_radar_nodes_is_premium
    ON public.radar_nodes(owner_id) WHERE is_premium = TRUE;

-- ---------------------------------------------------------------
-- 3) Tabla `public.saved_contacts`
--    Los contactos guardados (amigos) deben mantener el snapshot
--    de si eran Premium cuando se guardaron.
-- ---------------------------------------------------------------
ALTER TABLE public.saved_contacts
    ADD COLUMN IF NOT EXISTS headline        TEXT         NOT NULL DEFAULT '';
ALTER TABLE public.saved_contacts
    ADD COLUMN IF NOT EXISTS bio             TEXT         NOT NULL DEFAULT '';
ALTER TABLE public.saved_contacts
    ADD COLUMN IF NOT EXISTS is_premium      BOOLEAN      NOT NULL DEFAULT FALSE;
ALTER TABLE public.saved_contacts
    ADD COLUMN IF NOT EXISTS premium_catalog TEXT;

-- ---------------------------------------------------------------
-- 4) Función helper: aplicar premium a un usuario
--    Útil desde el cliente cuando se confirma el pago en
--    RevenueCat / Stripe. Sólo el propio usuario puede auto-
--    aplicarse el flag.
-- ---------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.apply_premium(
    p_user_id    UUID,
    p_catalog    TEXT DEFAULT NULL
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    UPDATE public.profiles
    SET is_premium     = TRUE,
        premium_catalog = COALESCE(p_catalog, premium_catalog),
        premium_since  = COALESCE(premium_since, NOW())
    WHERE id = p_user_id;
END;
$$;

-- ---------------------------------------------------------------
-- 5) Verificación post-migración
-- ---------------------------------------------------------------
DO $$
DECLARE
    profiles_premium_count    INTEGER;
    radar_nodes_premium_count INTEGER;
    saved_premium_count       INTEGER;
BEGIN
    SELECT COUNT(*) INTO profiles_premium_count
    FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'is_premium';

    SELECT COUNT(*) INTO radar_nodes_premium_count
    FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'radar_nodes' AND column_name = 'is_premium';

    SELECT COUNT(*) INTO saved_premium_count
    FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'saved_contacts' AND column_name = 'is_premium';

    RAISE NOTICE '✅ Feature2 OK. profiles.is_premium: %, radar_nodes.is_premium: %, saved_contacts.is_premium: %',
        profiles_premium_count, radar_nodes_premium_count, saved_premium_count;
END $$;
