package com.rlunaalc.rutify

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.rlunaalc.rutify.ui.Coordenada

class RetosFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var rutaAdapter: RetoAdapter
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

        rutaAdapter = RetoAdapter(listaRetos, "retos")
        recyclerView.adapter = rutaAdapter

        obtenerRetosDesdeFirebase()

        view.findViewById<Button>(R.id.btnIrPerfil).setOnClickListener {
            findNavController().navigate(R.id.action_retosFragment_to_perfilFragment)
        }
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
                    val nombreImagen = "reto_${nombreReto}.jpg"
                    val imagenRutaUrl = "https://lsowlungekgzqifulsoh.supabase.co/storage/v1/object/public/foto/$nombreImagen"

                    val ruta = Ruta(
                        nombre = nombreReto,
                        imagenRuta = imagenRutaUrl,
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
