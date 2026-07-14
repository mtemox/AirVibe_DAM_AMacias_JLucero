# Página web — Confirmación de correo (AirVibe)

Landing page estática que se muestra cuando el usuario hace clic en el enlace de verificación de Supabase Auth.

## Archivo

- `auth/confirm/index.html` — diseño alineado con onboarding/auth de la app (gradiente azul→coral, ola, tipografía Inter).

## Publicar (elegir una opción)

### GitHub Pages
1. Sube la carpeta `web/` al repo.
2. En GitHub → Settings → Pages → Source: carpeta `/web` o `/docs`.
3. URL resultante (ejemplo): `https://tu-usuario.github.io/AirVibe/auth/confirm/`

### Vercel / Netlify
1. Importa el repo.
2. **Root directory:** `web`
3. Deploy. URL (ejemplo): `https://airvibe.vercel.app/auth/confirm/`

### Supabase Storage (bucket público)
1. Crea bucket `public` con acceso de lectura.
2. Sube `auth/confirm/index.html`.
3. URL pública del archivo.

## Configurar Supabase

En **Authentication → URL Configuration**:

| Campo | Valor |
|-------|--------|
| **Site URL** | `https://TU-DOMINIO/auth/confirm/` |
| **Redirect URLs** | Añade la misma URL (y variantes sin barra final si aplica) |

En **Authentication → Email Templates → Confirm signup**, el enlace `{{ .ConfirmationURL }}` ya apunta al verify de Supabase; tras confirmar, redirige a tu **Site URL**.

No hace falta cambiar código Android: la app sigue usando `signUp` / `signIn` como antes.

## Probar en local

```bash
cd web
npx --yes serve .
# Abre http://localhost:3000/auth/confirm/
```

Para simular éxito: `http://localhost:3000/auth/confirm/#access_token=test&type=signup`
