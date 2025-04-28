package com.rlunaalc.rutify

import com.google.android.gms.maps.model.LatLng
import com.rlunaalc.rutify.ui.Coordenada

data class Ruta(
    val nombre: String = "",
    val coordenadas: List<Coordenada> = emptyList(),
    val imagenRuta: String = "",
    val usuario: String = "",
    val imagenUsuario: String = "",
    val rutaHecha: Boolean = false
)
