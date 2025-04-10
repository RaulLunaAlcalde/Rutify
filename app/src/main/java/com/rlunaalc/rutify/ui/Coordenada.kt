package com.rlunaalc.rutify.ui

import com.google.android.gms.maps.model.LatLng

data class Coordenada(
    val lat: Double = 0.0,
    val lng: Double = 0.0
) {
    // Constructor vacío requerido para Firebase
    constructor() : this(0.0, 0.0) // Constructor sin parámetros

    // Convierte la Coordenada en un LatLng
    fun toLatLng(): LatLng = LatLng(lat, lng)
}
