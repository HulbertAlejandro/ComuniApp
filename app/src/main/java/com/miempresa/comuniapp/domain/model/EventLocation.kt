package com.miempresa.comuniapp.domain.model

/**
 * Coordenadas geográficas de un Evento.
 * Estas son seleccionadas por el usuario tocando el mapa de Mapbox
 * durante la creación o edición de un evento.
 *
 * @param latitude  Latitud en grados decimales
 * @param longitude Longitud en grados decimales
 * @param directionDisplay Dirección legible obtenida por geocodificación inversa (opcional)
 */
data class EventLocation(
    val latitude: Double,
    val longitude: Double,
    val directionDisplay: String = ""
)