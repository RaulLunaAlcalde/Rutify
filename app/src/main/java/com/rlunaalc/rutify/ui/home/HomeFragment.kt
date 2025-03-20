package com.rlunaalc.rutify.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.rlunaalc.rutify.R
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var polylineManager: PolylineAnnotationManager

    private val selectedPoints = mutableListOf<Point>()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        mapView = view.findViewById(R.id.mapView)

        val editTextRouteName = view.findViewById<EditText>(R.id.editTextRouteName)
        val buttonSave = view.findViewById<Button>(R.id.buttonSave)

        val userEmail = auth.currentUser?.email ?: ""

        if (userEmail.isEmpty()) {
            Toast.makeText(requireContext(), "No estás autenticado", Toast.LENGTH_SHORT).show()
            return view
        }

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            val annotationPlugin = mapView.annotations
            polylineManager = annotationPlugin.createPolylineAnnotationManager()

            mapView.getMapboxMap().addOnMapClickListener { point ->
                selectedPoints.add(point)
                drawPolyline()
                Toast.makeText(requireContext(), "Punto: ${point.latitude()}, ${point.longitude()}", Toast.LENGTH_SHORT).show()
                true
            }
        }

        buttonSave.setOnClickListener {
            val routeName = editTextRouteName.text.toString()
            saveRouteToFirestore(routeName, selectedPoints, userEmail)
        }

        return view
    }

    private fun drawPolyline() {
        polylineManager.deleteAll()
        if (selectedPoints.size >= 2) {
            val polyline = PolylineAnnotationOptions()
                .withPoints(selectedPoints)
                .withLineColor("#FF0000")
                .withLineWidth(4.0)
            polylineManager.create(polyline)
        }
    }

    private fun saveRouteToFirestore(routeName: String, points: List<Point>, userEmail: String) {
        if (routeName.isBlank()) {
            Toast.makeText(requireContext(), "Introduce un nombre para la ruta", Toast.LENGTH_SHORT).show()
            return
        }

        if (points.isEmpty()) {
            Toast.makeText(requireContext(), "No hay puntos seleccionados", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = dateFormat.format(Date())

        val routeData = hashMapOf(
            "coordenada inicio" to hashMapOf(
                "lat" to points.first().latitude(),
                "lng" to points.first().longitude()
            ),
            "fecha" to date,
            "puntos" to points.map { point ->
                hashMapOf("lat" to point.latitude(), "lng" to point.longitude())
            }
        )

        firestore.collection("usuarios")
            .document(userEmail)
            .collection("rutas")
            .document(routeName)
            .set(routeData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Ruta guardada correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al guardar la ruta", Toast.LENGTH_SHORT).show()
            }
    }
}
