-- =========================================================================
-- Migración Opcional para Supabase: Mensajes No Leídos
-- 
-- NOTA IMPORTANTE: En la arquitectura actual de AirVibe, los mensajes 
-- de chat (tabla `chat_messages`) son P2P y offline-first. Actualmente 
-- NO se sincronizan con Supabase, por lo que esta migración en la nube 
-- NO es estrictamente necesaria para que la burbuja de mensajes funcione 
-- localmente.
-- 
-- Sin embargo, si en el futuro decides habilitar la copia de seguridad 
-- en la nube o sincronización de chats en Supabase, debes ejecutar 
-- este script en el SQL Editor de Supabase.
-- =========================================================================

-- Agregar la columna 'is_read' a la tabla chat_messages en Supabase (si existe)
DO $$
BEGIN
    IF EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_schema = 'public' AND table_name = 'chat_messages'
    ) THEN
        ALTER TABLE public.chat_messages
        ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT true;
        
        -- Crear un índice para optimizar las consultas de mensajes no leídos
        CREATE INDEX IF NOT EXISTS index_chat_messages_is_read 
        ON public.chat_messages(is_read);
    END IF;
END
$$;
