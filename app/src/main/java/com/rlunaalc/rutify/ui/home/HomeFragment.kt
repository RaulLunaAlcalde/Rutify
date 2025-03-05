package com.rlunaalc.rutify.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.rlunaalc.rutify.R

import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val mapViewportState = rememberMapViewportState()
                val selectedPoints = remember { mutableStateListOf<Point>() }
                var routeName by remember { mutableStateOf("") }
                val context = LocalContext.current

                val userEmail = auth.currentUser?.email ?: ""

                if (userEmail.isEmpty()) {
                    Toast.makeText(context, "No estás autenticado", Toast.LENGTH_SHORT).show()
                    return@setContent
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // MapboxMap como fondo
                    MapboxMap(
                        modifier = Modifier.fillMaxSize(),
                        mapViewportState = mapViewportState,
                        onMapClickListener = { point ->
                            selectedPoints.add(point)
                            Toast.makeText(context, "Punto seleccionado: ${point.latitude()}, ${point.longitude()}", Toast.LENGTH_SHORT).show()
                            true
                        }
                    )

                    // Coloca los elementos de la UI sobre el mapa
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        // Contenedor horizontal para el campo de nombre de la ruta y el botón
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            // Campo de nombre de la ruta
                            BasicTextField(
                                value = routeName,
                                onValueChange = { routeName = it },
                                modifier = Modifier
                                    .weight(1f) // El campo ocupa el 80% del espacio disponible
                                    .padding(8.dp)
                                    .background(Color(0xFF464646), shape = MaterialTheme.shapes.small)
                                    .heightIn(min = 40.dp), // Asegura una altura mínima para el campo de texto
                                textStyle = TextStyle(color = Color.White), // Cambia el color del texto a blanco
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.padding(8.dp)) {
                                        if (routeName.isEmpty()) {
                                            Text("Nombre de la ruta", color = Color.White) // Coloca el texto en blanco si está vacío
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            // Botón para guardar ruta
                            Button(
                                onClick = { saveRouteToFirestore(routeName, selectedPoints, context, userEmail) },
                                modifier = Modifier
                                    .align(Alignment.CenterVertically) // Alinea el botón verticalmente
                                    .padding(8.dp)
                                    .heightIn(min = 40.dp), // Asegura una altura mínima para el botón
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFF1ED760), // Color personalizado
                                    contentColor = Color.White // Color del texto en blanco
                                )
                            ) {
                                Text("Guardar Ruta")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveRouteToFirestore(routeName: String, selectedPoints: List<Point>, context: android.content.Context, userEmail: String) {
        if (routeName.isBlank()) {
            Toast.makeText(context, "Introduce un nombre para la ruta", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPoints.isEmpty()) {
            Toast.makeText(context, "No hay puntos seleccionados", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = dateFormat.format(Date())

        val routeData = hashMapOf(
            "coordenada inicio" to hashMapOf(
                "lat" to selectedPoints.first().latitude(),
                "lng" to selectedPoints.first().longitude()
            ),
            "fecha" to date,
            "puntos" to selectedPoints.map { point ->
                hashMapOf("lat" to point.latitude(), "lng" to point.longitude())
            }
        )

        firestore.collection("usuarios")
            .document(userEmail)
            .collection("rutas")
            .document(routeName)
            .set(routeData)
            .addOnSuccessListener {
                Toast.makeText(context, "Ruta guardada correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al guardar la ruta", Toast.LENGTH_SHORT).show()
            }
    }
}
