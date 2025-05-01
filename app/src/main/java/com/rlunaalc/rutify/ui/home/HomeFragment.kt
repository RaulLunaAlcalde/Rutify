package com.rlunaalc.rutify.ui.home

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rlunaalc.rutify.R
import com.rlunaalc.rutify.databinding.FragmentHomeBinding
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: FragmentHomeBinding
    private var points = mutableListOf<LatLng>()
    private lateinit var polyline: Polyline
    private val markers = mutableListOf<Marker>()

    @RequiresApi(35)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.drawRouteButton.setOnClickListener {
            if (points.size >= 2) {
                drawRoute(points)
            } else {
                Snackbar.make(requireView(), "Agrega al menos dos puntos", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.undoButton.setOnClickListener {
            if (points.isNotEmpty() && markers.isNotEmpty()) {
                points.removeLast()
                markers.removeLast().remove()
            }
        }

        binding.saveRouteButton.setOnClickListener {
            val nombreRuta = binding.routeNameEditText.text.toString().trim()
            if (nombreRuta.isEmpty()) {
                Snackbar.make(requireView(), "Por favor, ingrese un nombre para la ruta", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (points.isEmpty()) {
                Snackbar.make(requireView(), "No hay ruta para guardar", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            guardarRutaEnFirebase(nombreRuta, points)
        }

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE

        mMap.setOnMapClickListener { latLng ->
            points.add(latLng)
            val marker = mMap.addMarker(MarkerOptions().position(latLng).title("Punto"))
            if (marker != null) {
                markers.add(marker)
            }
        }
    }

    private fun drawRoute(points: List<LatLng>) {
        val start = points.first()
        val end = points.last()
        val apiKey = "AIzaSyCfGYR2vyhOTIdfH1KaKBv43wK8xOAwMiA"
        val waypoints = points.drop(1).dropLast(1).joinToString("|") { "${it.latitude},${it.longitude}" }
        val waypointsParam = if (waypoints.isNotEmpty()) "&waypoints=$waypoints" else ""
        val directionsUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=${start.latitude},${start.longitude}&destination=${end.latitude},${end.longitude}$waypointsParam&key=$apiKey"

        val queue = Volley.newRequestQueue(requireContext())
        val stringRequest = StringRequest(Request.Method.GET, directionsUrl,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val routes = jsonResponse.getJSONArray("routes")

                    if (routes.length() > 0) {
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
                            drawAnimatedPolyline(routePoints)
                            marcarInicioYFin(routePoints)
                            mostrarDistanciaTotal(routePoints)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error procesando respuesta Directions API", e)
                }
            },
            { error ->
                Log.e("HomeFragment", "Error en solicitud Directions API: $error")
            }
        )
        queue.add(stringRequest)
    }

    private fun drawAnimatedPolyline(routePoints: List<LatLng>) {
        if (::polyline.isInitialized) {
            polyline.remove()
        }

        val polylineOptions = PolylineOptions()
            .color(Color.BLUE)
            .width(8f)

        polyline = mMap.addPolyline(polylineOptions)

        val handler = Handler()
        var index = 0

        val runnable = object : Runnable {
            override fun run() {
                if (index < routePoints.size) {
                    val currentPoints = polyline.points.toMutableList()
                    currentPoints.add(routePoints[index])
                    polyline.points = currentPoints
                    index++
                    handler.postDelayed(this, 50) // velocidad de animación (ms)
                }
            }
        }
        handler.post(runnable)
    }

    private fun marcarInicioYFin(routePoints: List<LatLng>) {
        mMap.addMarker(
            MarkerOptions()
                .position(routePoints.first())
                .title("Inicio")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )

        mMap.addMarker(
            MarkerOptions()
                .position(routePoints.last())
                .title("Fin")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
    }

    private fun mostrarDistanciaTotal(points: List<LatLng>) {
        var distanciaTotal = 0.0
        for (i in 0 until points.size - 1) {
            distanciaTotal += distanciaEntreDosPuntos(points[i], points[i + 1])
        }

        val distanciaKm = distanciaTotal
        Snackbar.make(requireView(), "Distancia total: %.2f km".format(distanciaKm), Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.green))
            .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            .show()
    }

    private fun distanciaEntreDosPuntos(p1: LatLng, p2: LatLng): Double {
        val earthRadius = 6371 // km

        val latDiff = Math.toRadians(p2.latitude - p1.latitude)
        val lngDiff = Math.toRadians(p2.longitude - p1.longitude)
        val a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(p1.latitude)) * Math.cos(Math.toRadians(p2.latitude)) *
                Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
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
                "coordenadas" to coordenadas.map { hashMapOf("lat" to it.latitude, "lng" to it.longitude) },
                "rutaHecha" to false
            )

            usuarioRef.collection("rutas").document(nombreRuta).set(rutaData)
                .addOnSuccessListener {
                    Snackbar.make(requireView(), "Ruta guardada exitosamente", Snackbar.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("HomeFragment", "Error guardando ruta", e)
                    Snackbar.make(requireView(), "Error al guardar ruta", Snackbar.LENGTH_SHORT).show()
                }
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

            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5))
        }
        return poly
    }
}
