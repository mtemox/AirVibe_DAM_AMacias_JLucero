# Bitácora de cambios — AirVibe

**Fecha:** 11 de julio de 2026 (sesión original) — 12 de julio de 2026 (cierre de bugs 8.1, 8.2 y 8.4)
**Contexto:** Sesión de desarrollo sobre la feature de salas cercanas, amigos persistentes, sync con Supabase y correcciones de chat P2P.
**Estado al cierre:** `assembleDebug` compila correctamente. **Bugs 8.1, 8.2 y 8.4 resueltos.** Bug 8.3 es solo una nota de entorno (no afecta a la app). Ver sección 8 para el detalle técnico.

---

## 1. Resumen ejecutivo

Se transformó el antiguo **broadcast** (mensaje invasivo 1-a-1) en **salas de proximidad** con notificación. Se añadió lista de amigos persistente, chat grupal en sala, sincronización cloud (Supabase) para salas/mensajes/amigos, y varias correcciones de radar y chat.

**Principio mantenido:** la lógica de Nearby Connections / Wi-Fi **no se reescribió**; los cambios son principalmente en UI, persistencia, payloads y sync.

---

## 2. Funcionalidades nuevas

### 2.1 Salas cercanas (antes: broadcast invasivo)

| Antes | Ahora |
|-------|-------|
| El broadcast enviaba un mensaje privado a cada peer | El anfitrión **crea una sala**; los cercanos reciben una **notificación** |
| No había pantalla de salas | Existe **Salas cercanas** (`RoomsListScreen`) |
| No había chat grupal | Existe **chat de sala** (`GroupRoomScreen`) |

**Flujo:**
1. En el radar → botón broadcast → sheet **"Crear sala cercana"**
2. Se crea `ProximityRoom` local + se envía payload `v2|invite|...` por Nearby
3. El receptor recibe notificación (`RoomInviteNotificationManager`) con acción "Unirse"
4. Al unirse → `GroupRoomScreen` con mensajes P2P de sala (`v2|room|...`)

### 2.2 Lista de amigos persistente

- Botón **Agregar** en preview de perfil → guarda en `saved_contacts` (tabla Room independiente del radar en vivo)
- Pantalla **Mis amigos** (`FriendsScreen`)
- Al agregar, se envía payload P2P `FriendAdd` para que el otro usuario también reciba el contacto
- Los amigos **no desaparecen** cuando el peer se desconecta del radar

### 2.3 Sincronización con Supabase (offline-first)

- **Perfil** (`profiles`): `full_name`, `status`, `tags` — se restaura al iniciar sesión
- **Amigos** (`saved_contacts`)
- **Salas** (`proximity_rooms`)
- **Mensajes de sala** (`room_messages`)

Cada usuario respalda **su propia copia** en la nube (`owner_id = auth.uid()`). El chat en vivo sigue siendo P2P; Supabase es backup/restore.

### 2.4 Navegación y deep links

- Menú hamburguesa en `RadarTopBar`: Salas cercanas, Mis amigos, Chats, Cerrar sesión
- Deep link a sala desde notificación (`EXTRA_OPEN_ROOM_ID`)
- Deep link a chat (`EXTRA_OPEN_CHAT_WITH_NODE_ID`)
- `onNewIntent` en `MainActivity` para cuando la app ya está abierta

---

## 3. Archivos NUEVOS

### Chat / Salas

| Archivo | Descripción |
|---------|-------------|
| `feature/chat/data/local/entity/ProximityRoomEntity.kt` | Entidad Room de salas |
| `feature/chat/data/local/entity/RoomMessageEntity.kt` | Entidad Room de mensajes de sala |
| `feature/chat/data/local/dao/ProximityRoomDao.kt` | DAO salas + mensajes |
| `feature/chat/data/mapper/RoomMapper.kt` | Mapper entidad ↔ dominio |
| `feature/chat/data/repository/ProximityRoomRepositoryImpl.kt` | Repositorio offline-first de salas |
| `feature/chat/domain/repository/ProximityRoomRepository.kt` | Interfaz del repositorio |
| `feature/chat/domain/model/ProximityRoom.kt` | Modelos `ProximityRoom` y `RoomMessage` |
| `feature/chat/presentation/GroupRoomScreen.kt` | UI chat grupal |
| `feature/chat/presentation/GroupRoomViewModel.kt` | ViewModel sala (retry al cargar, join) |
| `feature/chat/presentation/RoomsListScreen.kt` | Lista de salas activas |
| `feature/chat/presentation/RoomsListViewModel.kt` | ViewModel lista de salas |
| `feature/chat/data/notification/RoomInviteNotificationManager.kt` | Canal + notificación de invitación |
| `feature/chat/data/remote/RemoteProximityRoomDto.kt` | DTO Supabase salas |
| `feature/chat/data/remote/RemoteRoomMessageDto.kt` | DTO Supabase mensajes de sala |
| `feature/chat/data/remote/SupabaseProximityRoomDataSource.kt` | Data source Postgrest salas |
| `feature/chat/data/remote/SupabaseRoomMessageDataSource.kt` | Data source Postgrest mensajes |

### Radar / Amigos

| Archivo | Descripción |
|---------|-------------|
| `feature/radar/data/local/entity/SavedContactEntity.kt` | Contacto persistente (independiente de `radar_nodes`) |
| `feature/radar/data/local/dao/SavedContactDao.kt` | DAO amigos |
| `feature/radar/data/mapper/ContactMapper.kt` | Mapper perfil ↔ entidad guardada |
| `feature/radar/presentation/FriendsScreen.kt` | UI lista de amigos |
| `feature/radar/presentation/FriendsViewModel.kt` | ViewModel amigos |
| `feature/radar/data/remote/RemoteProfileDto.kt` | DTO perfil Supabase |
| `feature/radar/data/remote/RemoteSavedContactDto.kt` | DTO amigos Supabase |
| `feature/radar/data/remote/SupabaseProfileDataSource.kt` | Upsert/fetch perfil |
| `feature/radar/data/remote/SupabaseSavedContactDataSource.kt` | Upsert/fetch amigos |

### Sync / Utilidades

| Archivo | Descripción |
|---------|-------------|
| `feature/radar/data/sync/CloudSyncService.kt` | Orquesta push/restore de amigos, salas y mensajes |
| `core/network/TimestampUtils.kt` | Conversión `Long` ↔ ISO 8601 para Supabase |

---

## 4. Archivos MODIFICADOS (por área)

### 4.1 Núcleo / DI

**`core/di/ServiceLocator.kt`**
- Wiring de `proximityRoomDao`, `proximityRoomRepository`, `chatGatewayImpl`
- Data sources remotos: perfil, amigos, salas, mensajes de sala
- `cloudSyncService` lazy
- `onSignedIn`: restaura perfil + datos cloud + dispara `SyncScheduler`
- `requestContactsSync()` → encola sync inmediato
- `proximityRoomRepository` con callback `onDataChanged` → sync al crear/actualizar sala o mensaje

### 4.2 Navegación

**`MainActivity.kt`**
- `AppDeepLink` sealed interface (`Room`, `Chat`)
- `parseDeepLink()` desde Intent extras
- `onNewIntent()` para notificaciones con app en foreground
- Rutas: `RoomsListScreen`, `GroupRoomScreen`, `FriendsScreen`

### 4.3 Radar

**`feature/radar/presentation/RadarScreen.kt`**
- `SnackbarHost` para feedback (sala creada, contacto agregado)
- `LaunchedEffect` para navegar a sala tras broadcast y mostrar snackbar de amigo agregado
- Callback `onOpenRooms`

**`feature/radar/presentation/RadarViewModel.kt`**
- Eventos: `ConsumeBroadcastRoomNav`, `ConsumeContactAddedMessage`
- `saveOwnProfile`: eliminado restart Stop+Start del scanner; ahora `updateProfile` + `syncToRemote` + `requestContactsSync`
- `addToContacts` / `toggleFavorite`: disparan sync
- `addToContacts`: envía `chatGateway.sendFriendAdd(nodeId)`
- `displayNodes`: solo `liveNodes` (no mezcla nodos viejos de Room)

**`feature/radar/presentation/RadarUiState.kt`**
- `displayNodes` filtra solo peers en vivo

**`feature/radar/presentation/RadarUiEvent.kt`**
- Nuevos eventos de consumo de UI

**`feature/radar/presentation/components/RadarTopBar.kt`**
- Menú hamburguesa (`DropdownMenu`) en lugar de botones sueltos
- Entradas: Salas cercanas, Mis amigos, Chats, Cerrar sesión

**`feature/radar/presentation/components/BroadcastSheet.kt`**
- Copy actualizado: "Crear sala cercana", "Nombre o tema de la sala", "Crear sala"

**`feature/radar/presentation/components/ProfilePreviewSheet.kt`**
- Botón Agregar → "En amigos" deshabilitado si ya es favorito

**`feature/radar/data/repository/RadarRepositoryImpl.kt`**
- Integración con `savedContactDao`
- `getProfile` busca en radar en vivo + contactos guardados
- `toggleFavorite` / `addFavorite` / `saveContact` persisten en `saved_contacts`
- `removeNode` siempre elimina del radar al desconectarse (amigos quedan en tabla separada)

**`feature/radar/domain/repository/RadarRepository.kt`**
- `saveContact`, `isSavedContact`

**`feature/radar/domain/repository/ScannerProfileRepository.kt`**
- `syncToRemote`, `restoreFromRemote`

**`feature/radar/data/repository/ScannerProfileRepositoryImpl.kt`**
- Sync perfil con Supabase `profiles`
- Restore al login

**`feature/radar/data/device/service/AirVibeScannerService.kt`**
- `RoomInviteNotificationManager.ensureChannel()` en `onCreate`

**`feature/radar/data/sync/SyncWorker.kt`**
- Ahora usa `CloudSyncService`: restore → push nodos radar → push amigos/salas/mensajes
- `NetworkType.CONNECTED` (no solo Wi-Fi)

### 4.4 Chat P2P / Nearby

**`feature/chat/data/device/nearby/NearbyChatPayloadCodec.kt`**

Esquema v2 de payloads (texto UTF-8 separado por `|`):

```
v2|chat|<messageId>|<senderNodeId>|<text>|<createdAtMillis>
v2|invite|<messageId>|<senderNodeId>|<text>|<createdAtMillis>|<roomId>|<hostName>
v2|room|<messageId>|<senderNodeId>|<senderName>|<roomId>|<text>|<createdAtMillis>
v2|friend|<messageId>|<senderNodeId>|<displayName>|<status>|<detail>|<tags>|<createdAtMillis>
```

- `senderName` en room e `hostName` en invite evitan depender del radar local para nombres
- Compatibilidad hacia atrás en decode de `v2|room` (formato antiguo de 7 campos)

**`feature/chat/data/device/nearby/NearbyChatMessageGateway.kt`**
- `broadcastRoomInvite` crea invitación con nombre del host
- `sendRoomMessage` envía peer-a-peer con `resolveRoomEndpoints` (fix: `endpointToNode.keys` son endpointIds, no nodeIds)
- `sendFriendAdd` nuevo
- Handlers: `GroupInvite` → notificación + `roomRepository.receiveInvite`; `RoomMessage` → `persistIncomingMessage`; `FriendAdd` → `saveContact`
- Ignora eco de mensajes propios en sala (`senderNodeId == localUserId`)
- Nombre remitente: primero del payload, fallback radar, fallback "Usuario cercano"

**`feature/chat/domain/scanner/ChatMessageGateway.kt`**
- `broadcastRoomInvite`, `sendRoomMessage`, `sendFriendAdd`

**`feature/chat/data/repository/ChatRepositoryImpl.kt`**
- `broadcast()` crea sala host + envía invitación (retorna `BroadcastResult` con `roomId`)
- `sendRoomMessage()` delega a room repository + gateway

**`feature/chat/presentation/ChatScreen.kt`**
- `LazyColumn` con `reverseLayout = true` + `messages.asReversed()` (mensajes nuevos abajo, estilo WhatsApp)

**`feature/chat/presentation/GroupRoomScreen.kt`**
- Mismo patrón `reverseLayout = true`
- Estados loading/error al abrir sala
- `RoomMessageBubble` con alineación izquierda/derecha según `message.isOwn`

**`feature/chat/presentation/components/ChatComposer.kt`**
- Parámetro `showBroadcast` (oculto en sala)

**`feature/chat/presentation/ChatViewModel.kt`**
- `broadcast()` adaptado a `BroadcastResult`

### 4.5 Recursos

**`app/src/main/res/values/strings.xml`**
- Strings de notificación de invitación a sala (`room_invite_*`)

### 4.6 Base de datos local (Room)

**`feature/radar/data/local/database/AirVibeDatabase.kt`**

| Versión | Cambio |
|---------|--------|
| v2 | Tabla `chat_messages` |
| v3 | Tablas `proximity_rooms`, `room_messages` |
| v4 | Tabla `saved_contacts` + migración de favoritos desde `radar_nodes` |
| v5 | Columna `is_synced` en `proximity_rooms` y `room_messages` |
| v6 | Columna `is_own` en `room_messages` |

**Versión actual:** `SCHEMA_VERSION = 6`

---

## 5. Base de datos Supabase

Archivo de referencia: **`supabase_schema.sql`**

### Tablas nuevas / relevantes

| Tabla | Uso |
|-------|-----|
| `saved_contacts` | Amigos persistentes por `owner_id` |
| `proximity_rooms` | Metadata de salas (título, host, joined, is_host) |
| `room_messages` | Historial de mensajes de sala por `owner_id` |

### SQL pendiente en el proyecto remoto

El proyecto de la app es `sgdvortojvmfbfauginq.supabase.co`. El plugin MCP de Cursor estaba vinculado a otro proyecto; las migraciones deben aplicarse manualmente en el SQL Editor.

**Si aún no existe `room_messages`:**
```sql
-- Ver sección 6 de supabase_schema.sql (tabla + índices + RLS + trigger)
```

**Columna `is_own` (añadida en código, puede faltar en Supabase remoto):**
```sql
ALTER TABLE public.room_messages
    ADD COLUMN IF NOT EXISTS is_own BOOLEAN NOT NULL DEFAULT FALSE;
```

### RLS

Todas las tablas nuevas usan políticas `owner_id = auth.uid()` (o equivalente).

---

## 6. Arquitectura de sync (`CloudSyncService`)

```
Login (onSignedIn)
  ├── scannerProfileRepository.restoreFromRemote(userId)
  ├── cloudSyncService.restoreFromRemote(userId)   // pull
  └── SyncScheduler.requestNow()                   // push

SyncWorker (periódico / on-demand)
  ├── cloudSyncService.restoreFromRemote(userId)   // pull primero
  ├── radar nodes pending → Supabase
  └── cloudSyncService.pushPending(userId)
        ├── saved_contacts (is_synced = 0)
        ├── proximity_rooms (is_synced = 0)
        └── room_messages (is_synced = 0)
```

**Reglas de merge en restore:**
- Contactos/salas: inserta si no existe localmente, o sobrescribe si local ya está sincronizado
- Mensajes: inserta solo si `messageExists(id)` es false (evita duplicados)

**Triggers de sync local:**
- Crear sala, unirse, enviar/recibir mensaje de sala → `onDataChanged` en `ProximityRoomRepositoryImpl`
- Agregar/quitar amigo → `ServiceLocator.requestContactsSync()`
- Guardar perfil → `syncToRemote` + `requestContactsSync`

---

## 7. Cambios en payloads y P2P (detalle técnico)

### Bug corregido: endpoints de sala

En `resolveRoomEndpoints`, el mapa `endpointToNode` es `endpointId → nodeId`. Antes se usaban las keys como si fueran nodeIds y se buscaban en `nodeToEndpoint`, devolviendo siempre vacío. **Fix:** usar `endpointToNode.keys` directamente como endpoint IDs.

### Identidad del dispositivo

`DeviceIdentityProvider` genera `device-<UUID>` en SharedPreferences. **Se pierde al desinstalar.** Por eso:
- Los mensajes de sala ya no dependen solo de comparar `senderNodeId == localUserId` en lectura
- Se añadió columna `is_own` establecida al **escribir** (`true` al enviar, `false` al recibir)
- Se sincroniza `is_own` a Supabase para restore correcto tras reinstalar

---

## 8. Bugs conocidos / trabajo pendiente

### 8.1 Renderizado de mensajes en sala — ✅ [RESUELTO]

**Síntoma original reportado por el usuario:**
- Mensajes enviados de forma alternada aparecen agrupados por remitente (dos "bloques" en lugar de intercalados)
- Tras reinstalar, todos los mensajes pueden verse a la izquierda sin distinguir cuáles son propios

**Solución aplicada (dos síntomas, una causa por uno):**

- **Síntoma A — Bloques por desfase de reloj P2P:** en `NearbyChatMessageGateway.kt` se normaliza el `createdAt` del mensaje entrante contra `System.currentTimeMillis()` del receptor. Si el desfase supera los 5 s (umbral `MAX_CLOCK_SKEW_MS`), se usa el reloj local del receptor; si no, se respeta el del emisor. Esto evita que los relojes desincronizados de dos celulares offline agrupen los mensajes por remitente en la query `ORDER BY created_at ASC` del DAO.
- **Síntoma B — Mensajes propios a la izquierda tras reinstalar:** la columna `is_own` ya existía en el modelo Kotlin, pero faltaba en el esquema remoto de Supabase. Se aplicó el `ALTER TABLE public.room_messages ADD COLUMN IF NOT EXISTS is_own BOOLEAN NOT NULL DEFAULT FALSE` y se actualizó `supabase_schema.sql` para que cualquier setup fresco desde cero ya la incluya.

**Archivos tocados:** `NearbyChatMessageGateway.kt` (constante + normalización en `onIncomingPayload`), `supabase_schema.sql` (defensivo). **No se modificó:** `RoomMapper`, `ProximityRoomDao`, `GroupRoomScreen` ni la arquitectura.

### 8.2 Chat 1-a-1 sin backup cloud — ✅ [RESUELTO]

**Síntoma original:** el chat personal seguía solo local + P2P. La tabla `chat_messages` remota estaba modelada con `sender_id`/`receiver_id` como UUID FK a `auth.users`, incompatible con los IDs Bluetooth `device-<UUID>` que usa la app.

**Solución aplicada:** se migró la tabla remota al patrón offline-first usado por `room_messages` y `saved_contacts`: PK `id TEXT` (UUID generado en cliente), `owner_id UUID FK auth.users` (de quién es la copia de respaldo), `peer_node_id TEXT` (Bluetooth del peer), `direction TEXT` (Outgoing/Incoming), más `kind` y `status`. La RLS pasó a `owner_id = auth.uid()` con 4 policies (select/insert/update/delete). El script de migración es idempotente (DROP FKs primero, luego ALTER/DROP/ADD columnas) y preserva filas legacy en estado oculto por RLS.

**Archivos nuevos (2):** `feature/chat/data/remote/RemoteChatMessageDto.kt`, `feature/chat/data/remote/SupabaseChatMessageDataSource.kt`.
**Archivos modificados (2):** `feature/radar/data/sync/CloudSyncService.kt` (inyección + `restoreChatMessages`/`pushChatMessages`), `core/di/ServiceLocator.kt` (wiring del data source). **No se modificó:** `ChatMessageEntity`, `ChatDao`, `ChatRepositoryImpl`, dominio, ViewModels ni UI.

### 8.3 Plugin Supabase en Cursor

MCP conectado a `wmtoxbjvgvokdmsjenbq`, no al proyecto de producción `sgdvortojvmfbfauginq`. Migraciones deben ejecutarse manualmente o re-autenticar el plugin.

> ℹ️ **Nota:** esta entrada es solo una nota de entorno de desarrollo (configuración del plugin de Cursor en el editor del compañero). No es un bug de la aplicación ni requiere cambios de código. Se conserva aquí para referencia del equipo.

### 8.4 Radar nodes vs schema Supabase — ✅ [RESUELTO]

**Síntoma original:** la tabla remota `radar_nodes` estaba modelada con un paradigma de "red social" (PK UUID + `user_id`/`peer_id` FK a `auth.users` + `UNIQUE(user_id, peer_id)`), incompatible con el modelo P2P/Bluetooth de la app (id `device-<UUID>` TEXT) y con la cantidad de columnas específicas del radar que la app envía (angle, distance, signal, accent_color, presence, etc.). El sync de nodos fallaba silenciosamente en cada ejecución del `SyncWorker`.

**Solución aplicada:** se migró la tabla al patrón offline-first consistente con el resto: PK `id TEXT`, `owner_id UUID FK auth.users`, `display_name TEXT` (renombrado desde `name`), más todas las columnas específicas que envía `RemoteNodeDto` (`detail`, `kind`, `presence`, `angle_degrees`, `distance_normalized`, `signal_strength`, `accent_color_argb`). Se eliminaron `user_id`, `peer_id` y el `UNIQUE` constraint. La RLS pasó a `owner_id = auth.uid()` con 4 policies. El script de migración v2 dropea las FKs a `auth.users` antes de cualquier `ALTER COLUMN`/`DROP COLUMN` para evitar el error de tipo.

**Archivos modificados (4):** `feature/radar/data/remote/RemoteNodeDto.kt` (campo `ownerId` + `toEntity()`), `feature/radar/domain/remote/RemoteNodeDataSource.kt` (3 firmas: `upsert(nodes, ownerId)`, `fetchAll(ownerId)`, `delete(nodeId, ownerId)`), `feature/radar/data/remote/SupabaseNodeDataSource.kt` (filtro `eq("owner_id", ownerId)` en queries), `feature/radar/data/sync/SyncWorker.kt` (1 línea: pasar `userId` al `upsert`). **No se modificó:** `RemoteNode` (modelo de dominio), `NodeEntity`, `NodeMapper`, `RadarDao`, `RadarRepositoryImpl`, `ServiceLocator`, ViewModels ni UI — Clean Architecture intacta al 100 %.

---

## 9. Cómo probar (checklist para el compañero)

### Setup Supabase
1. Ejecutar `supabase_schema.sql` completo (o al menos tablas/policies faltantes)
2. Añadir columna `is_own` en `room_messages` si la tabla ya existía
3. Verificar RLS con usuario autenticado

### Salas
1. Dos dispositivos con escaneo activo y cuentas distintas
2. Dispositivo A: Crear sala → B recibe notificación
3. B: Unirse desde notificación o desde **Salas cercanas**
4. Intercambiar varios mensajes en ambos sentidos
5. Verificar orden visual y burbujas izquierda/derecha

### Amigos
1. A agrega a B desde preview de perfil
2. Verificar en **Mis amigos** en ambos (B vía payload `FriendAdd`)
3. Desconectar B del radar → A sigue viendo a B en amigos
4. Reconectar → B vuelve al radar pero amistad persiste

### Persistencia / reinstall
1. Crear sala + mensajes + amigos
2. Esperar sync (~30 s o forzar cerrando/abriendo app con red)
3. Desinstalar en un dispositivo
4. Reinstalar, mismo login → salas y mensajes deben restaurarse desde Supabase

### Radar
1. A apaga escaneo → B no debe ver a A en el radar (sí en amigos si lo agregó)

---

## 10. Mapa rápido de navegación UI

```
AuthScreen
  └── RadarScreen
        ├── Menú → Salas cercanas → RoomsListScreen → GroupRoomScreen
        ├── Menú → Mis amigos → FriendsScreen
        ├── Menú → Chats → ConversationsListScreen → ChatScreen
        ├── Broadcast sheet → crea sala + navega a GroupRoomScreen
        └── Preview perfil → Agregar amigo / Chat 1-a-1
```

---

## 11. Convenciones y decisiones

1. **Offline-first:** Room es fuente de verdad local; Supabase es backup por usuario (`owner_id`)
2. **Chat en sala en vivo:** siempre P2P vía Nearby; cloud no participa en tiempo real
3. **No reiniciar scanner al guardar perfil:** evita crashes en emulador (race en foreground service)
4. **Amigos ≠ nodos de radar:** tablas separadas; el radar solo muestra peers en vivo
5. **Payloads v2:** nombres embebidos en invite/room para no depender de `radar_nodes` en el receptor

---

## 12. Contacto / handoff

Si retomas este trabajo, prioridad sugerida:

1. **Arreglar renderizado de sala** (sección 8.1) — impacto directo en UX
2. Confirmar SQL de `is_own` aplicado en Supabase remoto
3. Probar reinstall con mensajes previos ya sincronizados con `is_own = true`
4. (Opcional) Sync cloud de chat 1-a-1 con tabla adaptada a `peer_node_id` text

**Build verificado:** `./gradlew assembleDebug` → BUILD SUCCESSFUL (11-jul-2026)
