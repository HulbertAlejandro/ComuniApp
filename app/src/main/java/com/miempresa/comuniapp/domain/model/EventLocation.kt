package com.miempresa.comuniapp.domain.model

/**
 * Coordenadas geográficas de un evento, seleccionadas en el mapa.
 * Firestore requiere constructor vacío, de ahí los valores por defecto.
 *
 * @param latitude         Latitud en grados decimales.
 * @param longitude        Longitud en grados decimales.
 * @param directionDisplay Dirección legible obtenida por geocodificación inversa.
 */
data class EventLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val directionDisplay: String = ""
)