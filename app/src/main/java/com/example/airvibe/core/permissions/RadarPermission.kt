package com.example.airvibe.core.permissions

import android.Manifest
import android.os.Build
import com.example.airvibe.R

/**
 * Catálogo tipado de los permisos que necesita la app para activar
 * el radar. Cada permiso incluye un `titleRes` y un `rationaleRes`
 * para alimentar la UI sin filtrar literales a la capa de
 * presentación.
 *
 * La función [allForCurrentSdk] devuelve el subconjunto de
 * permisos que **realmente** deben solicitarse en este dispositivo.
 * Esto evita pedir `BLUETOOTH_SCAN` en Android 7 (donde no existe
 * y la app crashearía).
 */
enum class RadarPermission(
    val androidPermission: String,
    val titleRes: Int,
    val rationaleRes: Int,
    val minSdk: Int = 1,
) {
    BLUETOOTH_SCAN(
        androidPermission = Manifest.permission.BLUETOOTH_SCAN,
        titleRes = R.string.permission_title_bluetooth_scan,
        rationaleRes = R.string.permission_rationale_scan,
        minSdk = Build.VERSION_CODES.S,
    ),
    BLUETOOTH_ADVERTISE(
        androidPermission = Manifest.permission.BLUETOOTH_ADVERTISE,
        titleRes = R.string.permission_title_bluetooth_advertise,
        rationaleRes = R.string.permission_rationale_advertise,
        minSdk = Build.VERSION_CODES.S,
    ),
    BLUETOOTH_CONNECT(
        androidPermission = Manifest.permission.BLUETOOTH_CONNECT,
        titleRes = R.string.permission_title_bluetooth_connect,
        rationaleRes = R.string.permission_rationale_connect,
        minSdk = Build.VERSION_CODES.S,
    ),
    ACCESS_FINE_LOCATION(
        androidPermission = Manifest.permission.ACCESS_FINE_LOCATION,
        titleRes = R.string.permission_title_location,
        rationaleRes = R.string.permission_rationale_location,
    ),
    NEARBY_WIFI_DEVICES(
        androidPermission = Manifest.permission.NEARBY_WIFI_DEVICES,
        titleRes = R.string.permission_title_wifi,
        rationaleRes = R.string.permission_rationale_wifi,
        minSdk = Build.VERSION_CODES.TIRAMISU,
    ),
    POST_NOTIFICATIONS(
        androidPermission = Manifest.permission.POST_NOTIFICATIONS,
        titleRes = R.string.permission_title_notifications,
        rationaleRes = R.string.permission_rationale_notifications,
        minSdk = Build.VERSION_CODES.TIRAMISU,
    );

    /** ¿Es obligatorio para la versión actual de Android? */
    fun isRequired(): Boolean = Build.VERSION.SDK_INT >= minSdk
}

object RadarPermissionSet {

    /** Permisos que se solicitan al activar el radar por primera vez. */
    val REQUIRED: List<RadarPermission> = RadarPermission.entries
        .filter { it.isRequired() && it != RadarPermission.POST_NOTIFICATIONS }
        .sortedBy { it.minSdk }

    /** Permisos que se piden de forma opcional, antes de mostrar la notificación. */
    val OPTIONAL: List<RadarPermission> = RadarPermission.entries
        .filter { it == RadarPermission.POST_NOTIFICATIONS && it.isRequired() }

    /** Devuelve el listado completo (REQUIRED + OPTIONAL) adecuado a la versión del OS. */
    fun allForCurrentSdk(): List<RadarPermission> =
        RadarPermission.entries.filter { it.isRequired() }
}
