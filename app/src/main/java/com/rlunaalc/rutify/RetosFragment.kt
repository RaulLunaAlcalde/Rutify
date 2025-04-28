package com.rlunaalc.rutify

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.rlunaalc.rutify.ui.Coordenada

class RetosFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var rutaAdapter: RutaAdapter
    private val db = FirebaseFirestore.getInstance()
    private val listaRetos = mutableListOf<Ruta>() // Seguimos usando Ruta

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_retos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewRetos)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        rutaAdapter = RutaAdapter(listaRetos, "retos")
        recyclerView.adapter = rutaAdapter

        obtenerRetosDesdeFirebase()
    }

    private fun obtenerRetosDesdeFirebase() {
        db.collection("retos").get()
            .addOnSuccessListener { documentos ->
                listaRetos.clear()
                for (documento in documentos) {
                    val nombreReto = documento.id // Usamos el ID como nombre
                    val puntos = documento.get("puntos") as? List<Map<String, Any>> ?: emptyList()

                    val coordenadas = puntos.map { punto ->
                        Coordenada(
                            lat = (punto["lat"] as Number).toDouble(),
                            lng = (punto["lng"] as Number).toDouble()
                        )
                    }

                    val ruta = Ruta(
                        nombre = nombreReto,
                        usuario = "Anónimo", // No hay creador en tu estructura, ponemos "Anónimo"
                        imagenRuta = "",
                        imagenUsuario = "",
                        coordenadas = coordenadas
                    )

                    Log.d("Firebase", "Reto obtenido: $nombreReto")
                    listaRetos.add(ruta)
                }
                rutaAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error obteniendo retos", e)
            }
    }
}
