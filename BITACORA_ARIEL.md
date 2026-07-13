# Bitácora de cambios UI/UX — AirVibe (Ariel)

**Fecha:** 12 de julio de 2026
**Contexto:** Integración completa de los diseños UI/UX estáticos (HTML/CSS/Tailwind) al entorno nativo de Android usando Jetpack Compose (Material 3). El objetivo principal fue rediseñar las pantallas respetando la regla estricta de **no tocar la lógica de negocio subyacente**.
**Estado al cierre:** Compilación exitosa (`assembleDebug`). Navegación inferior funcional entre módulos visuales. Chat P2P y lógica de backend intactos.

---

## 1. Resumen ejecutivo

Se crearon las interfaces nativas en Jetpack Compose para las 4 secciones principales de la aplicación (`Radar/Chats`, `Services`, `Groups`, `Profile`) usando componentes modernos (Glassmorphism, gradientes suaves, Material 3) sin conectar temporalmente datos reales (uso de mocks). 

Para la pantalla de `ChatScreen`, que ya poseía lógica funcional conectada a Room y ViewModels, se hizo una **inyección de diseño**: se reemplazaron únicamente los componentes visuales (burbujas y barras) sin alterar el flujo de datos.

---

## 2. Funcionalidades nuevas (Visuales)

### 2.1 Pantallas base integradas
- **Navegación inferior (Bottom Bar):** Conectada en el `MainActivity` para renderizar las nuevas pantallas en lugar de textos temporales.
- **RadarChatsScreen:** División de pantalla entre el radar animado superior y la lista interactiva de chats inferior. Se agregaron modificadores `clickable` para abrir el chat real.
- **ServicesScreen:** Lista de expertos con "chips" superiores filtrables y diseño en tarjetas limpias.
- **GroupsScreen:** Buscador superior flotante y lista de "broadcasts/salas" activas cercanas. Corrección de los espacios en blanco del margen superior (Scaffold insets).
- **ProfileScreen:** Avatar central, métricas rápidas (viajes, estrellas, amigos), y opciones de ajustes en lista.

### 2.2 Rediseño del Chat (Estilo iOS / WhatsApp)
- **TopBar:** Fondo azul sólido (`#305CDE`) reemplazando el estilo translúcido anterior. Ícono principal cambiado de relámpago a conexión Bluetooth.
- **Burbujas:** Colores sólidos (`#4166F5` para enviados, `#F0F5F9` para recibidos).
- **Fondo:** Se generó y agregó un patrón de figuras de olas (`wave_pattern.jpg`) como fondo inmersivo del chat, simulando el estilo de WhatsApp.

---

## 3. Archivos NUEVOS

| Archivo | Descripción |
|---------|-------------|
| `feature/services/presentation/ServicesScreen.kt` | Pantalla visual de Servicios (Descubrir expertos). Incluye listado de cards con ratings y chips. |
| `feature/groups/presentation/GroupsScreen.kt` | Pantalla visual de Grupos/Salas. Incluye barra de búsqueda superior y botón flotante (FAB). |
| `feature/profile/presentation/ProfileScreen.kt` | Pantalla de perfil de usuario. Incluye botones rápidos y lista de configuración. |
| `feature/radar/presentation/RadarChatsScreen.kt` | Pantalla híbrida de Radar (mitad superior) y Chats recientes (mitad inferior). Reemplaza al antiguo `ConversationsListScreen` visualmente. |
| `res/drawable-nodpi/wave_pattern.jpg` | Recurso gráfico: patrón continuo de olas usado como fondo para los chats individuales. |

---

## 4. Archivos MODIFICADOS

| Archivo | Qué se hizo |
|---------|-------------|
| `MainActivity.kt` | Se enrutaron los slots del menú inferior (`servicesContent`, `groupsContent`, `profileContent`) hacia las nuevas pantallas `.kt` correspondientes. |
| `feature/chat/presentation/ChatScreen.kt` | Se reemplazó `AirVibeAmbientBackground` por el fondo dinámico de imagen (`wave_pattern.jpg`). Se añadieron los imports necesarios (`Image`, `ContentScale`, etc). |
| `feature/chat/presentation/components/ChatTopBar.kt` | Se cambiaron colores de fondo, íconos (se pasó de `Bolt` a `BluetoothConnected`) y márgenes. |
| `feature/chat/presentation/components/MessageBubble.kt` | Se eliminaron los `Brush.linearGradient` para pasar a colores sólidos (`SolidColor`) estilo iOS clásico. |

---

## 5. Trabajo Pendiente (Lo que falta)

Dado que esta iteración fue 100% enfocada a la interfaz de usuario (UI), queda pendiente realizar la conexión ("Hook up") con la lógica de negocio (UX/Data). 

### UI/UX faltante:
1. **Expansión del Radar:** Hacer que al tocar el componente del Radar en `RadarChatsScreen`, este ocupe el 100% de la pantalla para realizar búsquedas profundas, ocultando temporalmente la lista de chats.
2. **Animaciones de transición:** Añadir transiciones suaves (Fade in/out, Slide) al navegar entre las opciones del menú inferior.

### Lógica de negocio faltante (Conectar datos reales a la UI):
1. **ServicesScreen:** Conectar la lista de `mockServices` con un ViewModel que obtenga datos reales (ej. Supabase o Radar P2P). Darle funcionalidad a los botones de "Contactar".
2. **GroupsScreen:** Darle lógica al botón flotante `+` (FloatingActionButton) para invocar la ventana de "Crear nueva sala cercana" que usa el `GroupRoomViewModel`. Reemplazar `mockGroups` por las salas detectadas reales.
3. **ProfileScreen:** Sustituir la data quemada (Avatar genérico, nombre falso, métricas) por los datos del usuario logueado almacenados localmente o en `SupabaseProfileDataSource`. Darle acción al botón "Editar Perfil".
4. **RadarChatsScreen:** Asegurar que la lista de chats iterada provenga del flujo real del `ConversationsListViewModel` y no del listado estático `mockChats` (actualmente inyectamos un id de prueba para forzar la apertura visual del chat).
