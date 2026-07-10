-- =============================================================
-- AirVibe / AirBump - Supabase Schema
-- Offline-First Android App Backend (PostgreSQL 15+)
-- =============================================================

-- Required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================
-- 1) PROFILES  (extends auth.users)
-- =============================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id          UUID        PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name   TEXT        NOT NULL,
    status      TEXT,
    profession  TEXT,
    tags        TEXT[]      NOT NULL DEFAULT '{}',
    avatar_url  TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================================
-- 2) RADAR_NODES  (Bluetooth-discovered / saved contacts)
--    user_id -> the authenticated user who saved the contact
--    peer_id -> the discovered user (FK to auth.users)
-- =============================================================
CREATE TABLE IF NOT EXISTS public.radar_nodes (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    peer_id     UUID        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name        TEXT        NOT NULL,
    status      TEXT,
    tags        TEXT[]      NOT NULL DEFAULT '{}',
    is_favorite BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT radar_nodes_user_peer_unique UNIQUE (user_id, peer_id)
);

CREATE INDEX IF NOT EXISTS idx_radar_nodes_user_id   ON public.radar_nodes(user_id);
CREATE INDEX IF NOT EXISTS idx_radar_nodes_user_peer ON public.radar_nodes(user_id, peer_id);

-- =============================================================
-- 3) CHAT_MESSAGES  (offline-first message history)
--    sent_at   -> original timestamp from the client device
--    created_at -> server-side receive time (used for sync order)
-- =============================================================
CREATE TABLE IF NOT EXISTS public.chat_messages (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id   UUID        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    receiver_id UUID        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    content     TEXT        NOT NULL,
    is_synced   BOOLEAN     NOT NULL DEFAULT TRUE,
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chat_messages_participants_distinct CHECK (sender_id <> receiver_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation
    ON public.chat_messages(sender_id, receiver_id, sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_inbox
    ON public.chat_messages(receiver_id, sent_at DESC);

-- =============================================================
-- TRIGGER FUNCTION: auto-touch updated_at on every UPDATE
-- Critical for offline-first conflict resolution.
-- =============================================================
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_profiles_updated_at      ON public.profiles;
DROP TRIGGER IF EXISTS trg_radar_nodes_updated_at   ON public.radar_nodes;
DROP TRIGGER IF EXISTS trg_chat_messages_updated_at ON public.chat_messages;

CREATE TRIGGER trg_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

CREATE TRIGGER trg_radar_nodes_updated_at
    BEFORE UPDATE ON public.radar_nodes
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

CREATE TRIGGER trg_chat_messages_updated_at
    BEFORE UPDATE ON public.chat_messages
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- =============================================================
-- ROW LEVEL SECURITY
-- =============================================================
ALTER TABLE public.profiles      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.radar_nodes   ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;

-- ---------- profiles: a user can only read/write their own row ----------
DROP POLICY IF EXISTS "profiles_select_own" ON public.profiles;
DROP POLICY IF EXISTS "profiles_insert_own" ON public.profiles;
DROP POLICY IF EXISTS "profiles_update_own" ON public.profiles;
DROP POLICY IF EXISTS "profiles_delete_own" ON public.profiles;

CREATE POLICY "profiles_select_own"
    ON public.profiles FOR SELECT
    TO authenticated
    USING (id = auth.uid());

CREATE POLICY "profiles_insert_own"
    ON public.profiles FOR INSERT
    TO authenticated
    WITH CHECK (id = auth.uid());

CREATE POLICY "profiles_update_own"
    ON public.profiles FOR UPDATE
    TO authenticated
    USING (id = auth.uid())
    WITH CHECK (id = auth.uid());

CREATE POLICY "profiles_delete_own"
    ON public.profiles FOR DELETE
    TO authenticated
    USING (id = auth.uid());

-- ---------- radar_nodes: scoped to owner (user_id) ----------
DROP POLICY IF EXISTS "radar_nodes_select_own" ON public.radar_nodes;
DROP POLICY IF EXISTS "radar_nodes_insert_own" ON public.radar_nodes;
DROP POLICY IF EXISTS "radar_nodes_update_own" ON public.radar_nodes;
DROP POLICY IF EXISTS "radar_nodes_delete_own" ON public.radar_nodes;

CREATE POLICY "radar_nodes_select_own"
    ON public.radar_nodes FOR SELECT
    TO authenticated
    USING (user_id = auth.uid());

CREATE POLICY "radar_nodes_insert_own"
    ON public.radar_nodes FOR INSERT
    TO authenticated
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "radar_nodes_update_own"
    ON public.radar_nodes FOR UPDATE
    TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "radar_nodes_delete_own"
    ON public.radar_nodes FOR DELETE
    TO authenticated
    USING (user_id = auth.uid());

-- ---------- chat_messages: participants can read; only sender can insert ----------
DROP POLICY IF EXISTS "chat_messages_select_participant" ON public.chat_messages;
DROP POLICY IF EXISTS "chat_messages_insert_sender"     ON public.chat_messages;

CREATE POLICY "chat_messages_select_participant"
    ON public.chat_messages FOR SELECT
    TO authenticated
    USING (sender_id = auth.uid() OR receiver_id = auth.uid());

CREATE POLICY "chat_messages_insert_sender"
    ON public.chat_messages FOR INSERT
    TO authenticated
    WITH CHECK (sender_id = auth.uid());

-- =============================================================
-- AUTO-CREATE PROFILE ON SIGNUP
-- Guarantees a public.profiles row for every auth.users insert,
-- so the mobile app can always rely on a 1:1 profile row.
-- =============================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.profiles (id, full_name)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'full_name', split_part(NEW.email, '@', 1))
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_on_auth_user_created ON auth.users;

CREATE TRIGGER trg_on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
