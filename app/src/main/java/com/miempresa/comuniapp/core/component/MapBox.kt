package com.miempresa.comuniapp.core.component

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.miempresa.comuniapp.domain.model.Event
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.miempresa.comuniapp.R // Asegúrate de tener un icono aquí

private const val DEFAULT_LAT = 4.4687891
private const val DEFAULT_LON = -75.6491181
private const val DEFAULT_ZOOM = 8.0
private const val DETAIL_ZOOM = 13.0
private const val MARKER_TAP_THRESHOLD = 0.005 * 0.005

@Composable
fun MapBox(
    modifier: Modifier = Modifier,
    events: List<Event> = emptyList(),
    showMyLocationButton: Boolean = true,
    activateClick: Boolean = false,
    initialPoint: Point? = null,
    onMapClickListener: (Point) -> Unit = {},
    onEventMarkerClick: (eventId: String) -> Unit = {}
) {
    val permissionState = rememberLocationPermissionState()
    var puckEnabled by remember { mutableStateOf(false) }
    var followRequested by remember { mutableStateOf(false) }
    var clickedPoint by remember { mutableStateOf(initialPoint) }

    // ✅ Cargamos un icono por defecto para que los eventos sean VISIBLES
    // Puedes reemplazar R.drawable.red_marker por cualquier pin que tengas en res/drawable
    val markerIcon = rememberIconImage(
        key = "event_marker",
        painter = painterResource(id = R.drawable.star)
    )

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(initialPoint ?: Point.fromLngLat(DEFAULT_LON, DEFAULT_LAT))
            zoom(if (initialPoint != null) DETAIL_ZOOM else DEFAULT_ZOOM)
        }
    }

    Box(modifier = modifier) {
        MapboxMap(
            modifier = Modifier.matchParentSize(),
            mapViewportState = mapViewportState,
            onMapClickListener = { point ->
                if (activateClick) {
                    clickedPoint = point
                    onMapClickListener(point)
                } else {
                    val tappedEvent = events.firstOrNull { event ->
                        val dLat = event.eventLocation.latitude - point.latitude()
                        val dLon = event.eventLocation.longitude - point.longitude()
                        (dLat * dLat + dLon * dLon) < MARKER_TAP_THRESHOLD
                    }
                    if (tappedEvent != null) onEventMarkerClick(tappedEvent.id)
                    else onMapClickListener(point)
                }
                true
            }
        ) {
            if (permissionState.hasPermission) {
                MapEffect(Unit) { mapView ->
                    mapView.location.updateSettings {
                        locationPuck = createDefault2DPuck(withBearing = true)
                        enabled = true
                        puckBearing = PuckBearing.COURSE
                        puckBearingEnabled = true
                    }
                    puckEnabled = true
                }
            }

            if (puckEnabled && followRequested) {
                LaunchedEffect(followRequested) {
                    mapViewportState.transitionToFollowPuckState(
                        FollowPuckViewportStateOptions.Builder().zoom(DETAIL_ZOOM).build()
                    )
                    followRequested = false
                }
            }

            // ✅ Dibujamos los eventos correctamente para SDK 11
            // ✅ Dentro de MapboxMap { ... }
            events.forEach { event ->
                key(event.id) { // IMPORTANTE: Usa el ID único del evento
                    PointAnnotation(
                        point = Point.fromLngLat(event.eventLocation.longitude, event.eventLocation.latitude)
                    ) {
                        iconImage = markerIcon
                        iconSize = 0.1
                    }
                }
            }

            clickedPoint?.let { point ->
                PointAnnotation(point = point)
            }
        }

        if (showMyLocationButton) {
            FloatingActionButton(
                onClick = {
                    if (permissionState.hasPermission) followRequested = true
                    else permissionState.requestPermission()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
            }
        }
    }
}

// Clase de estado de permisos (Mantenla igual)
class LocationPermissionState(hasPermission: Boolean = false, val requestPermission: () -> Unit = {}) {
    var hasPermission by mutableStateOf(hasPermission)
}

@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
    val context = LocalContext.current
    val initiallyGranted = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    val state = remember { LocationPermissionState(hasPermission = initiallyGranted) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        state.hasPermission = granted
    }
    return remember(state.hasPermission) {
        LocationPermissionState(
            hasPermission = state.hasPermission,
            requestPermission = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
        )
    }
}