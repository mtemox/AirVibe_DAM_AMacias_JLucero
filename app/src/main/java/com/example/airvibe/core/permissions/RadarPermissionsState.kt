package com.example.airvibe.core.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Estado Compose que combina la información de los permisos del
 * radar y expone utilidades listas para la UI:
 *  - [allGranted] indica si el radar puede arrancar.
 *  - [missing] lista los permisos pendientes.
 *  - [requestAll] lanza el diálogo del sistema.
 *  - [openAppSettings] abre la pantalla de Ajustes del sistema.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Stable
class RadarPermissionsState internal constructor(
    val permissions: List<RadarPermission>,
    private val raw: com.google.accompanist.permissions.MultiplePermissionsState,
    private val context: Context,
) {

    val allGranted: Boolean
        get() = raw.allPermissionsGranted

    val missing: List<RadarPermission>
        get() = permissions.filter { perm ->
            raw.permissions.firstOrNull { it.permission == perm.androidPermission }
                ?.status?.isGranted != true
        }

    val shouldShowRationale: Boolean
        get() = raw.permissions.any { it.status.shouldShowRationale }

    fun requestAll() {
        raw.launchMultiplePermissionRequest()
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

/**
 * Composable que recuerda el estado de los permisos del radar. Se
 * debe llamar dentro de un `@Composable` (idealmente en la raíz de
 * la pantalla).
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberRadarPermissionsState(
    permissions: List<RadarPermission> = RadarPermissionSet.allForCurrentSdk(),
): RadarPermissionsState {
    val context = LocalContext.current
    val androidPermissions = remember(permissions) {
        permissions.map { it.androidPermission }
    }
    val raw = rememberMultiplePermissionsState(androidPermissions)
    return remember(permissions, raw, context) {
        RadarPermissionsState(permissions = permissions, raw = raw, context = context)
    }
}

/**
 * Estado para un único permiso. Útil para flujos donde se solicita
 * un permiso de forma aislada (por ejemplo, el de notificaciones
 * tras activar el scanner).
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberSingleRadarPermission(permission: RadarPermission): PermissionStateWrapper {
    val context = LocalContext.current
    val raw = rememberPermissionState(permission.androidPermission)
    var hasRequested by remember { mutableStateOf(false) }
    val status: PermissionStatus = raw.status
    val shouldShow: Boolean = status.shouldShowRationale || (status is PermissionStatus.Denied && hasRequested)

    return remember(raw, shouldShow, hasRequested) {
        PermissionStateWrapper(
            permission = permission,
            isGranted = status.isGranted,
            shouldShowRationale = shouldShow,
            request = {
                hasRequested = true
                raw.launchPermissionRequest()
            },
            openSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            },
        )
    }
}

@Stable
data class PermissionStateWrapper(
    val permission: RadarPermission,
    val isGranted: Boolean,
    val shouldShowRationale: Boolean,
    val request: () -> Unit,
    val openSettings: () -> Unit,
)

/** Helper de conveniencia para comprobar si un contexto es una Activity. */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
