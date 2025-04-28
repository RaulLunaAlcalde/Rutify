package com.rlunaalc.rutify

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rlunaalc.rutify.ui.Coordenada
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class RealizarRuta : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var ruta: Ruta? = null
    private var tiempoInicio: Long = 0

    private lateinit var contenedorInfo: LinearLayout
    private lateinit var tvNombreRuta: TextView
    private lateinit var tvKilometros: TextView
    private lateinit var btnMostrarInfo: Button
    private lateinit var btnIniciarRuta: Button
    private lateinit var mapView: MapView
    private lateinit var locationRequest: com.google.android.gms.location.LocationRequest
    private lateinit var locationCallback: com.google.android.gms.location.LocationCallback
    private var marcadorUsuario: Marker? = null

    private val apiKey = "AIzaSyCfGYR2vyhOTIdfH1KaKBv43wK8xOAwMiA"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var isTracking = false
    private val rutaEnSeguimiento = mutableListOf<LatLng>()
    private var polyline: Polyline? = null  // Polyline global para actualizar sin crear una nueva

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_realizar_ruta, container, false)

        // Inicializar MapView
        mapView = rootView.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        contenedorInfo = view.findViewById(R.id.contenedorInfo)
        tvNombreRuta = view.findViewById(R.id.tvNombreRuta)
        tvKilometros = view.findViewById(R.id.tvKilometros)
        btnMostrarInfo = view.findViewById(R.id.btnMostrarInfo)
        btnIniciarRuta = view.findViewById(R.id.btnIniciarRuta)

        btnMostrarInfo.setOnClickListener {
            if (contenedorInfo.visibility == View.GONE) {
                contenedorInfo.visibility = View.VISIBLE
                btnMostrarInfo.text = "Ocultar Info"
            } else {
                contenedorInfo.visibility = View.GONE
                btnMostrarInfo.text = "Ver Info"
            }
        }

        btnIniciarRuta.setOnClickListener {
            if (isTracking) {
                detenerSeguimiento()
            } else {
                iniciarSeguimiento()
            }
        }

        val rutaName = arguments?.getString("rutaName") ?: return
        obtenerRuta(rutaName)
    }

    private fun iniciarSeguimiento() {
        isTracking = true
        btnIniciarRuta.text = "Detener Seguimiento"
        tiempoInicio = System.currentTimeMillis()

        locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            interval = 3000 // cada 3 segundos
            fastestInterval = 2000
            priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                for (location in locationResult.locations) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    rutaEnSeguimiento.add(latLng)

                    val rotation = if (rutaEnSeguimiento.size >= 2) {
                        calcularAngulo(rutaEnSeguimiento[rutaEnSeguimiento.size - 2], latLng)
                    } else {
                        0f
                    }

// Crear o actualizar marcador con rotación
                    if (marcadorUsuario == null) {
                        marcadorUsuario = mMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .anchor(0.5f, 0.5f)
                                .rotation(rotation)
                                .flat(true)
                                .icon(
                                    bitmapDescriptorFromVector(
                                        requireContext(),
                                        R.drawable.ic_start
                                    )
                                )
                                .title("Tu posición")
                        )
                    } else {
                        marcadorUsuario?.position = latLng
                        marcadorUsuario?.rotation = rotation
                    }

// Actualizar polyline
                    if (polyline == null) {
                        polyline = mMap.addPolyline(
                            PolylineOptions()
                                .addAll(rutaEnSeguimiento)
                                .width(8f)
                                .color(ContextCompat.getColor(requireContext(), R.color.green))
                        )
                    } else {
                        polyline?.points = rutaEnSeguimiento
                    }

                }
            }
        }

        // Verifica permisos y comienza actualizaciones
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    private fun calcularDistanciaTotal(): Double {
        var total = 0.0
        for (i in 0 until rutaEnSeguimiento.size - 1) {
            total += distanciaEntrePuntos(rutaEnSeguimiento[i], rutaEnSeguimiento[i + 1])
        }
        return total
    }


    private fun detenerSeguimiento() {
        isTracking = false
        btnIniciarRuta.text = "Iniciar Seguimiento"
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val duracionSegundos = ((System.currentTimeMillis() - tiempoInicio) / 1000).toInt()
        val distanciaKm = calcularDistanciaTotal()
        val ritmo = if (distanciaKm > 0) duracionSegundos / 60.0 / distanciaKm else 0.0
        val calorias = distanciaKm * 60

        val popup = requireView().findViewById<View>(R.id.popupResumenRuta)
        val tvDuracion = popup.findViewById<TextView>(R.id.tvPopupDuracion)
        val tvDistancia = popup.findViewById<TextView>(R.id.tvPopupDistancia)
        val tvRitmo = popup.findViewById<TextView>(R.id.tvPopupRitmo)
        val tvKcal = popup.findViewById<TextView>(R.id.tvPopupKcal)

        tvDuracion.text = "Duración: ${formatTiempo(duracionSegundos)}"
        tvDistancia.text = "Distancia: %.2f km".format(distanciaKm)
        tvRitmo.text = "Ritmo: %.2f min/km".format(ritmo)
        tvKcal.text = "Calorías: %.0f kcal".format(calorias)

        popup.visibility = View.VISIBLE
        popup.animate()
            .alpha(1f)
            .setDuration(600)
            .start()

        val btnVolverInicio = popup.findViewById<Button>(R.id.btnVolverInicio)
        btnVolverInicio.setOnClickListener {
            findNavController().navigate(R.id.homeDefFragment, null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.mobile_navigation, true)
                    .build())

        }
        val rutaName = arguments?.getString("rutaName")
        val auth = FirebaseAuth.getInstance()
        val userEmail = auth.currentUser?.email

        if (rutaName != null && userEmail != null) {
            val db = FirebaseFirestore.getInstance()
            val rutaRef = db.collection("usuarios").document(userEmail).collection("rutas").document(rutaName)

            rutaRef.update("rutaHecha", true)
                .addOnSuccessListener {
                    Log.d("Firebase", "Ruta marcada como completada correctamente.")
                    // 🔥 NUEVO: Mostrar Snackbar de confirmación
                    Snackbar.make(requireView(), "Ruta completada y guardada", Snackbar.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error actualizando rutaHecha", e)
                }

        }

    }

    private fun formatTiempo(segundos: Int): String {
        val minutos = segundos / 60
        val segundosRestantes = segundos % 60
        return "%02d:%02d".format(minutos, segundosRestantes)
    }


    private fun obtenerRuta(rutaName: String) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val usuarioEmail = auth.currentUser?.email ?: return
        val usuarioRef = db.collection("usuarios").document(usuarioEmail)

        usuarioRef.collection("rutas").document(rutaName).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val ruta = document.toObject(Ruta::class.java)
                    if (ruta != null) {
                        this.ruta = ruta
                        obtenerRutaOptimizada(ruta.coordenadas.map { it.toLatLng() })
                        mostrarInfoRuta(ruta)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error al obtener ruta", e)
            }
    }

    private fun mostrarInfoRuta(ruta: Ruta) {
        tvNombreRuta.text = "Nombre: ${ruta.nombre}"
        tvKilometros.text = "Kilómetros: ${"%.2f".format(calcularKilometros(ruta.coordenadas))}"
    }

    private fun obtenerRutaOptimizada(coordenadas: List<LatLng>) {
        if (coordenadas.size < 2) return

        val origen = "${coordenadas.first().latitude},${coordenadas.first().longitude}"
        val destino = "${coordenadas.last().latitude},${coordenadas.last().longitude}"
        val waypoints =
            coordenadas.drop(1).dropLast(1).joinToString("|") { "${it.latitude},${it.longitude}" }

        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=$origen&destination=$destino" +
                "&waypoints=$waypoints" +
                "&mode=walking&key=$apiKey"

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("RealizarRuta", "Error obteniendo la ruta: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    val puntosRuta = parsearRutaDesdeJSON(body)
                    activity?.runOnUiThread {
                        dibujarRutaEnMapa(puntosRuta)
                    }
                }
            }
        })
    }

    private fun calcularAngulo(from: LatLng, to: LatLng): Float {
        val deltaLon = Math.toRadians(to.longitude - from.longitude)
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)

        val y = Math.sin(deltaLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon)

        val angulo = Math.toDegrees(Math.atan2(y, x))
        return ((angulo + 360) % 360).toFloat()
    }


    private fun bitmapDescriptorFromVector(
        context: Context,
        @DrawableRes vectorResId: Int
    ): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(context, vectorResId)!!
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    private fun parsearRutaDesdeJSON(json: String): List<LatLng> {
        val puntos = mutableListOf<LatLng>()
        val jsonObj = JSONObject(json)
        val rutas = jsonObj.getJSONArray("routes")

        if (rutas.length() > 0) {
            val legs = rutas.getJSONObject(0).getJSONArray("legs")
            for (i in 0 until legs.length()) {
                val steps = legs.getJSONObject(i).getJSONArray("steps")
                for (j in 0 until steps.length()) {
                    val polyline =
                        steps.getJSONObject(j).getJSONObject("polyline").getString("points")
                    puntos.addAll(decodePolyline(polyline))
                }
            }
        }

        return puntos
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
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

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }

        return poly
    }

    private fun dibujarRutaEnMapa(ruta: List<LatLng>) {
        if (!::mMap.isInitialized) return

        val polylineOptions = PolylineOptions()
            .color(R.color.green)
            .width(8f)
            .addAll(ruta)

        mMap.addPolyline(polylineOptions)

        if (ruta.isNotEmpty()) {
            mMap.addMarker(MarkerOptions().position(ruta.first()).title("Inicio"))
            if (ruta.size > 1) {
                mMap.addMarker(MarkerOptions().position(ruta.last()).title("Fin"))
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ruta.first(), 15f))
        }
    }

    private fun calcularKilometros(coordenadas: List<Coordenada>): Double {
        var total = 0.0
        for (i in 0 until coordenadas.size - 1) {
            val start = coordenadas[i].toLatLng()
            val end = coordenadas[i + 1].toLatLng()
            total += distanciaEntrePuntos(start, end)
        }
        return total
    }

    private fun distanciaEntrePuntos(start: LatLng, end: LatLng): Double {
        val R = 6371 // Radio de la Tierra en km
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)

        val dlat = lat2 - lat1
        val dlon = lon2 - lon1

        val a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dlon / 2) * Math.sin(dlon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c // Resultado en kilómetros
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0 ?: return
        mMap.uiSettings.isZoomControlsEnabled = true
    }
}
