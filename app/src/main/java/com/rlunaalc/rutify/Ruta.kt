package com.rlunaalc.rutify

data class Ruta(
    val nombre: String = "",
    val coordenadas: List<Map<String, Double>> = emptyList(),
    val imagenRuta: String = "",
    val usuario: String = "",
    val imagenUsuario: String = ""
)