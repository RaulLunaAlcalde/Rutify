package com.rlunaalc.rutify.ui.home

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.rlunaalc.rutify.R
import com.rlunaalc.rutify.databinding.FragmentHomeBinding
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    var points = mutableListOf<LatLng>()
    private lateinit var polyline: Polyline
    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        val homeFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        homeFragment.getMapAsync(this)

        binding.drawRouteButton.setOnClickListener {
            if (points.size >= 2) {
                drawRoute(points)
            } else {
                Log.e("MapFragment", "No hay suficientes puntos para trazar la ruta.")
            }
        }

        // Botón para guardar la ruta
        binding.saveRouteButton.setOnClickListener {
            val nombreRuta = binding.routeNameEditText.text.toString().trim()

            if (nombreRuta.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, ingrese un nombre para la ruta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (points.isEmpty()) {
                Toast.makeText(requireContext(), "No hay ruta para guardar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            guardarRutaEnFirebase(nombreRuta, points)
        }

        return binding.root
    }

    private fun drawRoute(points: List<LatLng>) {
        if (points.size < 2) {
            Log.e("MapFragment", "No hay suficientes puntos para trazar la ruta.")
            return
        }

        val start = points.first()
        val end = points.last()
        Log.d("MapFragment", "Solicitando ruta de $start a $end")

        val apiKey = "AIzaSyCfGYR2vyhOTIdfH1KaKBv43wK8xOAwMiA"
        val waypoints = points.drop(1).dropLast(1).joinToString("|") { "${it.latitude},${it.longitude}" }
        val waypointsParam = if (waypoints.isNotEmpty()) "&waypoints=$waypoints" else ""
        val directionsUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=${start.latitude},${start.longitude}&destination=${end.latitude},${end.longitude}$waypointsParam&key=$apiKey"

        // Log para verificar la URL generada
        Log.d("MapFragment", "URL de Directions: $directionsUrl")

        val queue = Volley.newRequestQueue(requireContext())
        val stringRequest = StringRequest(Request.Method.GET, directionsUrl,
            { response ->
                try {
                    // Log para ver la respuesta completa de la API
                    Log.d("MapFragment", "Respuesta de la API: $response")

                    val jsonResponse = JSONObject(response)
                    val routes = jsonResponse.getJSONArray("routes")

                    if (routes.length() == 0) {
                        Log.e("MapFragment", "No se encontraron rutas en la respuesta de la API.")
                        return@StringRequest
                    }

                    val routePoints = mutableListOf<LatLng>()
                    val legs = routes.getJSONObject(0).getJSONArray("legs")
                    for (i in 0 until legs.length()) {
                        val steps = legs.getJSONObject(i).getJSONArray("steps")
                        for (j in 0 until steps.length()) {
                            val polyline = steps.getJSONObject(j).getJSONObject("polyline")
                            val decodedPoints = decodePoly(polyline.getString("points"))
                            routePoints.addAll(decodedPoints)
                        }
                    }

                    if (routePoints.isNotEmpty()) {
                        Log.d("MapFragment", "Dibujando ruta con ${routePoints.size} puntos.")
                        drawPolyline(routePoints)
                    } else {
                        Log.e("MapFragment", "La API no devolvió puntos de ruta válidos.")
                    }

                } catch (e: Exception) {
                    Log.e("MapFragment", "Error al procesar la respuesta de la API.", e)
                }
            },
            { error -> Log.e("MapFragment", "Error en la solicitud a la API: $error") }
        )

        queue.add(stringRequest)
    }

    private fun drawPolyline(routePoints: List<LatLng>) {
        if (routePoints.isEmpty()) {
            Log.e("MapFragment", "No hay puntos para dibujar la línea.")
            return
        }

        val polylineOptions = PolylineOptions()
            .color(Color.BLUE)
            .width(8f)
            .addAll(routePoints)

        if (::mMap.isInitialized) {
            polyline = mMap.addPolyline(polylineOptions)
            Log.d("MapFragment", "Línea dibujada con ${routePoints.size} puntos.")
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(routePoints.first(), 15f))
        } else {
            Log.e("MapFragment", "El mapa aún no está inicializado.")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE

        mMap.setOnMapClickListener { latLng ->
            points.add(latLng)
            Log.d("MapFragment", "Punto agregado: $latLng")
            mMap.addMarker(MarkerOptions().position(latLng).title("Punto"))
        }
    }

    private fun guardarRutaEnFirebase(nombreRuta: String, coordenadas: List<LatLng>) {
        val auth = FirebaseAuth.getInstance()
        val usuarioActual = auth.currentUser

        if (usuarioActual != null) {
            val usuarioEmail = usuarioActual.email ?: return
            val usuarioRef = FirebaseFirestore.getInstance().collection("usuarios").document(usuarioEmail)

            val rutaData = hashMapOf(
                "nombre" to nombreRuta,
                "fecha" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                "coordenadas" to coordenadas.map { hashMapOf("lat" to it.latitude, "lng" to it.longitude) }
            )

            usuarioRef.collection("rutas").document(nombreRuta).set(rutaData)
                .addOnSuccessListener { Log.d("Firebase", "Ruta '$nombreRuta' guardada exitosamente en Firestore") }
                .addOnFailureListener { e -> Log.e("Firebase", "Error guardando la ruta", e) }
        } else {
            Log.e("Firebase", "No hay usuario autenticado")
        }
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlat = if (result and 1 != 0) {
                (result shr 1).inv()
            } else {
                result shr 1
            }
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlng = if (result and 1 != 0) {
                (result shr 1).inv()
            } else {
                result shr 1
            }
            lng += dlng

            poly.add(LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5))
        }
        return poly
    }
}
