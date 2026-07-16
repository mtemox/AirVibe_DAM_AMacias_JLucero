<div align="center">

# 📡 AirVibe

### *Tu radar social y de servicios — en el mundo real, en tiempo real, sin internet.*

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](#)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.02-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](#)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-FF6F00?style=for-the-badge&logo=android&logoColor=white)](#)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-1976D2?style=for-the-badge&logo=android&logoColor=white)](#)
[![License](https://img.shields.io/badge/License-Private-lightgrey?style=for-the-badge)](#)
[![Supabase](https://img.shields.io/badge/Backend-Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)](#)

> *"Discover. Connect. Vibe."*

</div>

---

## 📑 Tabla de Contenidos

1. [🚀 Acerca del Proyecto](#-acerca-del-proyecto)
2. [✨ Características Principales](#-características-principales)
3. [🧱 Tech Stack](#-tech-stack)
4. [🏗️ Arquitectura](#-arquitectura)
5. [📂 Estructura del Proyecto](#-estructura-del-proyecto)
6. [⚙️ Instalación y Configuración](#-instalación-y-configuración)
7. [▶️ Ejecución (Despliegue Local)](#-ejecución-despliegue-local)
8. [🔐 Permisos del Sistema](#-permisos-del-sistema)
9. [🗄️ Base de Datos y Backend (Supabase)](#-base-de-datos-y-backend-supabase)
10. [🎨 Design System](#-design-system)
11. [🧪 Pruebas](#-pruebas)
12. [🐛 Bitácoras y Documentación Interna](#-bitácoras-y-documentación-interna)
13. [👥 Autores / Integrantes del Equipo](#-autores--integrantes-del-equipo)
14. [📄 Licencia](#-licencia)

---

## 🚀 Acerca del Proyecto

**AirVibe** es una aplicación Android nativa que reinventa la forma en que las personas descubren, conectan y contratan servicios a su alrededor. Utiliza **Nearby Connections** (Bluetooth + Wi-Fi Direct) para detectar dispositivos cercanos incluso **sin conexión a internet**, complementado con un backend en **Supabase** para sincronización, autenticación y persistencia en la nube.

### 🎯 ¿Qué problema resuelve?

En un mundo hiperconectado seguimos sin poder encontrar fácilmente:
- 👤 **Personas afines** cerca nuestro (red social de proximidad).
- 🔧 **Profesionales y servicios** locales disponibles en el momento.
- 💬 **Conversaciones efímeras y de baja fricción** sin necesidad de intercambiar números telefónicos o estar en la misma red Wi-Fi.

AirVibe lo resuelve con un **radar de proximidad en tiempo real**, un **chat P2P offline**, **salas grupales cercanas** y un **marketplace de expertos locales** — todo dentro de una interfaz moderna, animada y construida 100% con **Jetpack Compose** y **Material 3**.

---

## ✨ Características Principales

### 📡 Radar de Proximidad
- 🛰️ **Escaneo en tiempo real** vía Nearby Connections (Bluetooth + Wi-Fi Direct).
- 🎯 Visualización tipo **radar animado** con burbujas, anillos de distancia, *sweep* rotatorio y mapa de fondo procedural.
- 🟢 Indicador de **presencia** (En línea · Disponible · Ocupado · Ausente · Emergencia).
- 🔁 **Foreground Service** dedicado (`AirVibeScannerService`) que mantiene el radar vivo aunque la app esté en background, con notificación persistente y canal dedicado.
- 🎛️ **Controles granulares**: iniciar/detener escaneo, *broadcast*, filtros, ver radar completo o sólo Chats.

### 👋 Conexión Social (Handshake)
- 🤝 **Solicitudes de conexión** bidireccionales (`HandshakeRequest`) con aceptar/rechazar.
- 🪟 **Sheet nativo** de solicitud con perfil del emisor, foto, *headline* y *tags*.
- 🔔 **Notificaciones con acciones** (`Accept` / `Reject`) que funcionan incluso con la app cerrada, procesadas por un `BroadcastReceiver` (`HandshakeActionReceiver`).
- 📋 **Lista persistente de amigos** (`SavedContact`) que no desaparece cuando un peer se desconecta del radar.

### 💬 Chat P2P + Salas Grupales
- 🗨️ **Chat 1-a-1 offline** con burbujas estilo iOS/WhatsApp, fondo de olas inmersivo y auto-scroll.
- 👥 **Salas cercanas (Proximity Rooms)**: crea una sala, invita a los peers cercanos con notificación push y chatea en grupo vía P2P.
- 📨 **Notificaciones de invitación a sala** con acción *Unirse* y deep-link directo.
- 🟢 Estado de mensajes (enviando, enviado, recibido) y borrado offline.

### 🔍 Servicios / Marketplace
- 🛠️ Pantalla **Servicios** con chips filtrables, cards de profesionales (rating, reseñas, distancia, disponibilidad).
- 🧑‍💼 Soporte para **perfiles Premium** con catálogo de precios (Feature 2) y *headline* + *bio* extendidos.
- 💼 Categorías: tecnología, hogar, diseño, salud, etc.

### 👤 Perfil y Onboarding
- 🪪 **Perfil editable** con nombre, estado, *tags*, foto (cámara o subida).
- 📸 **Foto de perfil** con `FileProvider`, compresión local (`ImageCompressor`) y subida a Supabase Storage.
- 🧭 **Onboarding** visual de 3 pasos antes del login.
- 🚪 **Cerrar sesión** desde el drawer lateral.

### 🎨 UI / UX
- 🌗 **Modo claro / oscuro / sistema** con persistencia en `DataStore` (`AppTheme`).
- 🪟 **Glassmorphism** (efectos de cristal) en cards, pills, botones y barras.
- 🎬 **Animaciones fluidas**: `AnimatedContent` entre rutas, `Crossfade` entre tabs, splash con logo pulsante, radar con `rememberInfiniteTransition`.
- 🍎 Estética **shadcn/ui + Apple** con tipografía Inter, gradientes suaves y acentos vibrantes.
- ♿ **Edge-to-edge** con `enableEdgeToEdge` y manejo de insets de teclado (`windowSoftInputMode="adjustResize"`).

### ☁️ Sync en la Nube (Offline-First)
- 🔄 **Sincronización diferida** con `WorkManager` (`SyncWorker`).
- 📤 Push automático de nodos, amigos, salas y mensajes pendientes.
- 📥 **Restore** al iniciar sesión desde Supabase.
- 📊 **Telemetría** de vistas de perfil para analíticas Premium.
- 🧠 **Reglas de merge** anti-duplicados (inserta si no existe; sobrescribe sólo si ya estaba sincronizado).

### 🔐 Autenticación
- 📧 **Email + Password** con confirmación por correo.
- 🔗 **Deep-link de confirmación** redirigido a landing page en Vercel (`airvibe.vercel.app/auth/confirm/`).
- 🪪 Estado de sesión reactivo (`AuthStatus: Loading · SignedOut · SignedIn`).

---

## 🧱 Tech Stack

### 🟣 Lenguaje y Build
| Tecnología | Versión | Descripción |
|---|---|---|
| ![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white) | `2.2.10` | Lenguaje principal. |
| ![Gradle](https://img.shields.io/badge/Gradle-9.3.1-02303A?style=flat-square&logo=gradle&logoColor=white) | `9.3.1` | Build system con wrapper. |
| ![AGP](https://img.shields.io/badge/AGP-9.1.1-0A4D5C?style=flat-square&logo=android&logoColor=white) | `9.1.1` | Android Gradle Plugin. |
| ![KSP](https://img.shields.io/badge/KSP-2.0.2-4285F4?style=flat-square) | `2.2.10-2.0.2` | Procesamiento de anotaciones (Room). |
| ![JDK](https://img.shields.io/badge/JDK-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white) | `17` | Compilación target y source. |
| ![JVM Target](https://img.shields.io/badge/JVM-17-007396?style=flat-square&logo=java&logoColor=white) | `17` | Bytecode generado. |

### 🎨 UI / Compose
| Tecnología | Versión | Descripción |
|---|---|---|
| Jetpack Compose BOM | `2026.02.01` | Toolchain declarativa. |
| Material 3 | (BOM) | Design system moderno. |
| Material Icons Extended | (BOM) | Set completo de iconos. |
| Compose UI Text Google Fonts | (BOM) | Carga de tipografías (Inter). |
| Compose Tooling | (BOM) | Preview + debugging. |
| Accompanist Permissions | `0.36.0` | Manejo de permisos en Compose. |

### 🧩 Arquitectura y Concurrencia
| Tecnología | Versión | Descripción |
|---|---|---|
| AndroidX Lifecycle (Runtime / ViewModel / Compose) | `2.9.4` | MVVM reactivo con `collectAsStateWithLifecycle`. |
| AndroidX Activity Compose | `1.8.0` | `ComponentActivity` con `setContent`. |
| Kotlinx Coroutines (core / android / play-services) | `1.10.2` | Programación asíncrona estructurada. |
| Kotlinx Serialization | `1.9.0` | Serialización JSON para DTOs. |
| AndroidX WorkManager | `2.10.5` | Tareas en background (sync). |
| Coil Compose | `2.6.0` | Carga de imágenes. |

### 🗄️ Persistencia Local
| Tecnología | Versión | Descripción |
|---|---|---|
| Room Runtime + KTX | `2.8.4` | ORM con `Flow` reactivo. |
| Room Compiler (KSP) | `2.8.4` | Generación de DAOs. |
| AndroidX Core KTX | `1.10.1` | Extensiones Kotlin. |
| Desugar JDK Libs | `2.1.5` | Soporte Java 8+ en minSdk 24. |

### ☁️ Backend (BaaS)
| Tecnología | Versión | Descripción |
|---|---|---|
| ![Supabase](https://img.shields.io/badge/Supabase-3.6.0-3ECF8E?style=flat-square&logo=supabase&logoColor=white) | `3.6.0` | Auth + PostgREST + Storage. |
| Supabase BOM | `3.6.0` | Bill of materials. |
| Supabase Core / Postgrest / Auth / Storage | (BOM) | Módulos del SDK. |
| Ktor Client OkHttp | `3.0.1` | HTTP subyacente. |

### 📡 Proximidad
| Tecnología | Versión | Descripción |
|---|---|---|
| Play Services Nearby | `19.3.0` | Bluetooth + Wi-Fi Direct P2P. |

---

## 🏗️ Arquitectura

AirVibe implementa una **Clean Architecture por features** con enfoque **MVVM + MVI ligero** y un **Service Locator** central para la inyección de dependencias.

```
┌─────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                      │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐ │
│  │ Compose UI  │←→│ ViewModel    │←→│ UiState (data class)│ │
│  └─────────────┘  └──────────────┘  └─────────────────────┘ │
│         ↑                ↑                                   │
│         │                ▼                                   │
│         │       (UiEvent)                                   │
└─────────┼────────────────────────────────────────────────────┘
          │
┌─────────▼────────────────────────────────────────────────────┐
│                        DOMAIN LAYER                          │
│  ┌──────────────────┐  ┌──────────────────────────────────┐  │
│  │ Repository (ifc) │  │ Models · Use cases · State       │  │
│  └──────────────────┘  └──────────────────────────────────┘  │
└─────────┬────────────────────────────────────────────────────┘
          │
┌─────────▼────────────────────────────────────────────────────┐
│                          DATA LAYER                          │
│  ┌─────────────────────┐    ┌─────────────────────────────┐   │
│  │ Repository (impl)   │───→│ Local: Room (entities, DAO) │   │
│  │                     │    └─────────────────────────────┘   │
│  │                     │    ┌─────────────────────────────┐   │
│  │                     │───→│ Remote: Supabase PostgREST  │   │
│  │                     │    └─────────────────────────────┘   │
│  │                     │    ┌─────────────────────────────┐   │
│  │                     │───→│ Device: Nearby P2P Gateway  │   │
│  │                     │    └─────────────────────────────┘   │
│  └─────────────────────┘                                     │
└──────────────────────────────────────────────────────────────┘
          │
┌─────────▼────────────────────────────────────────────────────┐
│                    CORE / DESIGN SYSTEM                      │
│  Theme · Colors · Typography · Shapes · GlassTokens          │
│  ServiceLocator · SupabaseClientFactory · Permissions        │
│  UserFeedback (Snackbar) · WorkManager Configuration         │
└──────────────────────────────────────────────────────────────┘
```

### Principios clave:
- ✅ **Single source of truth** — `StateFlow` inmutable expuesto por cada `ViewModel`.
- ✅ **Unidirectional data flow** — UI emite `UiEvent` → ViewModel procesa → produce nuevo `UiState`.
- ✅ **Offline-first** — Room es la fuente local; Supabase es backup/restore.
- ✅ **Reactive** — `Flow` + `collectAsStateWithLifecycle` para una UI siempre sincronizada.
- ✅ **Separation of concerns** — Cero acceso directo a datos desde la UI.

---

## 📂 Estructura del Proyecto

```
AirVibe/
├── app/
│   ├── build.gradle.kts                  # Módulo app (Compose, Supabase, Room, Nearby)
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml          # Permisos Bluetooth, Wi-Fi, Cámara, FGS
│       ├── java/com/example/airvibe/
│       │   ├── AirVibeApplication.kt    # Entry point + ServiceLocator init
│       │   ├── MainActivity.kt          # Navegación raíz + deep-links + theme
│       │   ├── core/                    # Design system, DI, network, preferences
│       │   │   ├── designsystem/        # Theme · Color · Type · Shapes · GlassTokens
│       │   │   ├── di/                  # ServiceLocator
│       │   │   ├── network/             # SupabaseClientFactory · SupabaseConfig
│       │   │   ├── permissions/         # RadarPermission · RadarPermissionsState
│       │   │   ├── preferences/         # AppTheme (Light/Dark/System)
│       │   │   ├── ui/feedback/         # Snackbar · UserMessage
│       │   │   └── util/                # ImageCompressor · ColorUtils
│       │   └── feature/                 # 🎯 Features aisladas
│       │       ├── auth/                # 🔐 Login · Signup · Onboarding · Splash
│       │       ├── radar/               # 📡 Radar P2P + Handshake + Friends
│       │       ├── chat/                # 💬 Chat 1-a-1 + Salas + Notificaciones
│       │       ├── groups/              # 👥 Salas cercanas (UI)
│       │       ├── services/            # 🔍 Marketplace de expertos
│       │       ├── profile/             # 👤 Perfil + Edición
│       │       └── main/                # 🏠 Bottom navigation + Drawer
│       └── res/                         # Strings · Themes · Drawables · Fonts
├── gradle/libs.versions.toml            # Catálogo de versiones (Version Catalog)
├── supabase_schema.sql                  # Schema inicial de la BD
├── update_schema_feature{2..5}.sql       # Migraciones incrementales
├── update_schema_unread_messages.sql
├── web/                                 # Landing de confirmación de email (Vercel)
│   └── auth/confirm/index.html
├── BITACORA.md                          # 📘 Bitácora técnica
├── BITACORA_ARIEL.md                    # 📗 Bitácora UI/UX
├── local.properties                     # ⚠️ No commitear (SDK path)
├── settings.gradle.kts
├── build.gradle.kts
└── gradle.properties
```

---

## ⚙️ Instalación y Configuración

### 📋 Requisitos Previos

| Herramienta | Versión mínima | Verificar con |
|---|---|---|
| **Android Studio** | Ladybug 2024.2.1+ (recomendado Koala+) | `File → About` |
| **JDK** | 17 | `java -version` |
| **Android SDK** | API 36 (compile/target) · 24 (min) | SDK Manager |
| **Gradle** | 9.3.1 (incluido via wrapper) | `./gradlew --version` |
| **Git** | 2.30+ | `git --version` |

> ☝️ El proyecto usa **Kotlin 2.2.10** y **AGP 9.1.1** — asegúrate de que Android Studio sea compatible.

### 1️⃣ Clonar el Repositorio

```bash
git clone https://github.com/tu-usuario/AirVibe.git
cd AirVibe
```

### 2️⃣ Configurar el SDK Local

Android Studio genera `local.properties` automáticamente, pero si clonas sin abrirlo, créalo manualmente:

**`local.properties`** (⚠️ **NO commitear este archivo** — ya está en `.gitignore`)
```properties
sdk.dir=/ruta/a/tu/Android/Sdk
# En Windows:
# sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
```

> 💡 La forma más sencilla: **abrir el proyecto en Android Studio** y dejar que regenere el archivo.

### 3️⃣ Configurar Supabase (Backend)

> 🔐 **Importante:** Las credenciales de Supabase **ya están cableadas** en el código (clase `SupabaseConfig`) porque el proyecto se compila con una instancia compartida del equipo. Si vas a usar tu propio proyecto Supabase, sigue estos pasos:

#### Opción A — Usar el proyecto compartido (recomendado para devs)
No requiere configuración adicional. La app se conectará a `https://sgdvortojvmfbfauginq.supabase.co`.

#### Opción B — Usar tu propio proyecto Supabase

1. **Crea un proyecto** en [supabase.com](https://supabase.com).

2. **Aplica el schema** ejecutando los archivos SQL en orden en el **SQL Editor**:
   ```sql
   -- 1) Schema base
   \i supabase_schema.sql

   -- 2) Migraciones incrementales
   \i update_schema_feature2.sql
   \i update_schema_feature3.sql
   \i update_schema_feature4.sql
   \i update_schema_feature5.sql
   \i update_schema_unread_messages.sql
   ```

3. **Crea un bucket público** llamado `avatars` en **Storage** (para las fotos de perfil).

4. **Configura Auth**:
   - `Authentication → Providers → Email` → habilitado.
   - `Authentication → URL Configuration`:
     - **Site URL:** `https://tu-dominio.com/auth/confirm/`
     - **Redirect URLs:** añade tu URL de confirmación.

5. **Edita** `app/src/main/java/com/example/airvibe/core/network/SupabaseConfig.kt`:
   ```kotlin
   object SupabaseConfig {
       const val PROJECT_URL: String = "https://TU-PROYECTO.supabase.co"
       const val PUBLISHABLE_KEY: String = "sb_publishable_TU_CLAVE..."
       const val EMAIL_CONFIRM_REDIRECT_URL: String = "https://tu-dominio.com/auth/confirm/"
       // ...
   }
   ```

### 4️⃣ Configurar Variables de Entorno (Opcional)

Si prefieres **no hardcodear** las credenciales, puedes migrarlas a `local.properties` y leerlas desde `BuildConfig`. Ejemplo en `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        val localProps = java.util.Properties().apply {
            load(rootProject.file("local.properties").inputStream())
        }
        buildConfigField("String", "SUPABASE_URL", "\"${localProps["SUPABASE_URL"]}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${localProps["SUPABASE_KEY"]}\"")
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}
```

Y en `local.properties`:
```properties
SUPABASE_URL=https://TU-PROYECTO.supabase.co
SUPABASE_KEY=sb_publishable_TU_CLAVE...
```

> ⚠️ Recuerda añadir `buildConfig = true` a `buildFeatures` y `buildConfigField` al manifest si lo haces.

### 5️⃣ Sincronizar Gradle

```bash
./gradlew --refresh-dependencies
```

O desde Android Studio: `File → Sync Project with Gradle Files` (icono del elefante con flecha).

---

## ▶️ Ejecución (Despliegue Local)

### 🚀 Compilar el APK Debug

```bash
# Linux / macOS
./gradlew :app:assembleDebug

# Windows (PowerShell o CMD)
.\gradlew.bat :app:assembleDebug
```

El APK se generará en: `app/build/outputs/apk/debug/app-debug.apk`

### 📱 Instalar en un Emulador

1. Abre **Android Studio** → `Device Manager` → crea un AVD (API 24+).
2. Inicia el emulador.
3. Ejecuta:
   ```bash
   ./gradlew :app:installDebug
   ```
   O desde Android Studio: `Run ▶` (Shift+F10) con el dispositivo seleccionado.

### 📲 Instalar en un Dispositivo Físico

1. **Habilita Opciones de desarrollador** en tu Android: `Ajustes → Acerca del teléfono → Toca 7 veces "Número de compilación"`.
2. **Activa Depuración USB**: `Ajustes → Opciones de desarrollador → Depuración USB`.
3. Conecta el dispositivo vía USB y acepta el diálogo *Allow USB debugging*.
4. Verifica la conexión:
   ```bash
   adb devices
   ```
5. Ejecuta:
   ```bash
   ./gradlew :app:installDebug
   adb shell am start -n com.example.airvibe/.MainActivity
   ```

### 🧪 Comandos Útiles

```bash
# Limpiar el proyecto
./gradlew clean

# Compilar y correr tests unitarios
./gradlew test

# Compilar y correr tests instrumentados
./gradlew connectedAndroidTest

# Ver logs en vivo (filtrado por tag)
adb logcat -s AirVibe:V Radar:V Nearby:V

# Inspeccionar la base de datos Room
adb shell "run-as com.example.airvibe cat databases/airvibe.db" > airvibe.db
```

> 💡 **Tip:** Para probar el radar P2P necesitas **2 dispositivos reales** (el emulador no soporta Nearby Connections con otros dispositivos fuera del host).

---

## 🔐 Permisos del Sistema

La app declara y solicita los siguientes permisos en `AndroidManifest.xml`:

| Permiso | API | Propósito |
|---|---|---|
| `INTERNET` | Todas | Comunicación con Supabase. |
| `CAMERA` | Todas | Tomar foto de perfil. |
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | ≤ 30 | Nearby legacy. |
| `BLUETOOTH_SCAN` / `BLUETOOTH_ADVERTISE` / `BLUETOOTH_CONNECT` | 31+ | Nearby moderno (Android 12+). |
| `NEARBY_WIFI_DEVICES` | 33+ | Wi-Fi Direct (Android 13+). |
| `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE` | Todas | Estado Wi-Fi para Nearby. |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Todas | Requerido por Bluetooth clásico. |
| `POST_NOTIFICATIONS` | 33+ | Notificaciones de foreground service. |
| `FOREGROUND_SERVICE` | 28+ | Mantener radar vivo. |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | 34+ | Sub-tipo para conexiones BT (Android 14+). |

Los permisos de Bluetooth, ubicación y notificaciones se solicitan en runtime desde la UI mediante **`Accompanist Permissions`**, con pantallas de rationale dedicadas y deep-link a Ajustes si el usuario los deniega permanentemente.

---

## 🗄️ Base de Datos y Backend (Supabase)

### 📊 Tablas Principales

| Tabla | Descripción |
|---|---|
| `radar_nodes` | Nodos del radar del usuario (estado, presencia, *tags*, *favorito*). |
| `profiles` | Perfil extendido (full_name, status, tags, headline, bio, premium). |
| `saved_contacts` | Amigos persistentes (no se borran al desconectarse). |
| `proximity_rooms` | Salas de chat grupal cercanas. |
| `room_messages` | Mensajes de sala por `owner_id`. |
| `room_members` | Guests activos por sala. |
| `handshake_requests` | Solicitudes de conexión (estado: Pending/Accepted/Rejected). |
| `profile_views` | Telemetría de vistas de perfil. |
| `chat_messages` | Mensajes 1-a-1 (espejo offline-first). |

> 🔒 Todas las tablas usan **RLS (Row Level Security)** con políticas `auth.uid() = owner_id` — el cliente solo ve sus propios datos.

### 💾 Base Local (Room · v14)

```
NodeEntity              ProfileViewEntity
SavedContactEntity      HandshakeRequestEntity
ChatMessageEntity       ProximityRoomEntity
RoomMessageEntity       RoomMemberEntity
```

El esquema local es **offline-first**: la app funciona 100% sin internet. Supabase es backup/restore.

### ☁️ Storage

- **Bucket `avatars`** (público) — para fotos de perfil subidas desde la cámara o galería.

---

## 🎨 Design System

El **core/designsystem/** define los tokens visuales reutilizables:

- 🎨 **`Color.kt`** — Paleta `AirVibeLightColors` + `AirVibeDarkColors` (Material 3 ColorScheme).
- 🔤 **`Type.kt`** — Tipografía `AirVibeTypography` (Inter vía Google Fonts).
- 📐 **`Shapes.kt`** — `AirVibeShapes` (radius semánticos).
- 💎 **`GlassTokens.kt`** — Tokens de glassmorphism para light/dark.

### 🧩 Componentes Reutilizables

| Componente | Descripción |
|---|---|
| `GlassCard` | Card con efecto de cristal. |
| `GlassPill` | Pill / chip translúcido. |
| `LiquidGlassButton` | Botón con gradiente animado. |
| `GlassTextField` | Input con glassmorphism. |
| `AvatarMonogram` | Avatar con iniciales + color derivado. |
| `StatusDot` | Indicador de presencia. |
| `WaveHeader` | Header con ola decorativa. |
| `AirVibeAmbientBackground` | Fondo animado para el chat. |

---

## 🧪 Pruebas

```bash
# Tests unitarios JVM
./gradlew :app:testDebugUnitTest

# Tests instrumentados (requiere dispositivo)
./gradlew :app:connectedDebugAndroidTest

# Lint
./gradlew :app:lintDebug
```

Stack de testing:
- **JUnit 4** (`4.13.2`) para tests JVM.
- **AndroidX Test / Espresso** (`3.5.1`) para tests instrumentados.
- **Compose UI Test** (`androidx.compose.ui:ui-test-junit4`) para tests de UI.

---

## 🐛 Bitácoras y Documentación Interna

- 📘 **[`BITACORA.md`](./BITACORA.md)** — Bitácora técnica detallada: arquitectura de sync, migraciones de Room, payloads Nearby v2, deep-links, y resolución de bugs 8.1, 8.2 y 8.4.
- 📗 **[`BITACORA_ARIEL.md`](./BITACORA_ARIEL.md)** — Bitácora UI/UX: rediseño de pantallas, glassmorphism, sistema de diseño, integración de mocks y trabajo pendiente.
- 📜 [`supabase_schema.sql`](./supabase_schema.sql) — Schema inicial de la BD remota.
- 📜 `update_schema_feature{2..5}.sql` — Migraciones incrementales por feature.
- 🌐 [`web/`](./web/) — Landing estática de confirmación de email (deployable en Vercel/Netlify/GitHub Pages).

---

## 👥 Autores / Integrantes del Equipo

<div align="center">

### 🌟 Equipo de Desarrollo AirVibe 🌟

</div>

<table align="center">
  <tr>
    <td align="center" width="50%">
      <a href="https://github.com/ariel-macias">
        <img src="https://img.shields.io/badge/GitHub-Ariel%20Macias-181717?style=for-the-badge&logo=github&logoColor=white" alt="Ariel Macias"/>
      </a>
      <br /><br />
      <strong>🧑‍🎨 Ariel Macias</strong>
      <br />
      <em>UI/UX Designer & Frontend Engineer</em>
      <br /><br />
      <sub>
        ✦ Diseño de interfaces (glassmorphism, animaciones)<br />
        ✦ Integración de Compose / Material 3<br />
        ✦ Sistema de diseño (tema, tipografía, color)<br />
        ✦ Estética shadcn/ui + Apple<br />
        ✦ Mockups → Producción
      </sub>
    </td>
    <td align="center" width="50%">
      <a href="https://github.com/juan-lucero">
        <img src="https://img.shields.io/badge/GitHub-Juan%20Lucero-181717?style=for-the-badge&logo=github&logoColor=white" alt="Juan Lucero"/>
      </a>
      <br /><br />
      <strong>⚙️ Juan Lucero</strong>
      <br />
      <em>Backend & Mobile Engineer</em>
      <br /><br />
      <sub>
        ✦ Arquitectura MVVM + Service Locator<br />
        ✦ Integración Nearby Connections (P2P)<br />
        ✦ Sincronización Supabase (offline-first)<br />
        ✦ Persistencia Room (v14, 14 migraciones)<br />
        ✦ Foreground Service + WorkManager
      </sub>
    </td>
  </tr>
</table>

<div align="center">

> 🤝 *"La combinación de un diseño impecable con una arquitectura sólida es lo que hace grande a una app."*

</div>

---

## 📄 Licencia

Este proyecto es de carácter **privado / académico**. Todos los derechos reservados © 2026 — Ariel Macias & Juan Lucero.

---

<div align="center">

**Hecho con ❤️ · ☕ · y mucho 📡 Kotlin en Ecuador**

⭐ *Si te gustó el proyecto, considera darle una estrella en GitHub.* ⭐

</div>
