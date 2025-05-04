package com.rlunaalc.rutify

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HistorialFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var rutaAdapter: RutaAdapter
    private val listaRutas = mutableListOf<Ruta>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_historial, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        rutaAdapter = RutaAdapter(listaRutas, "historial")
        recyclerView.adapter = rutaAdapter

        obtenerRutasDesdeFirebase()

        return view
    }

    private fun obtenerRutasDesdeFirebase() {
        val auth = FirebaseAuth.getInstance()
        val usuarioActual = auth.currentUser

        if (usuarioActual != null) {
            val usuarioEmail = usuarioActual.email ?: return
            val db = FirebaseFirestore.getInstance()
            val usuarioRef = db.collection("usuarios").document(usuarioEmail)

            usuarioRef.get()
                .addOnSuccessListener { docUsuario ->
                    val imagenPerfil = docUsuario.getString("imagenPerfil") ?: ""

                    usuarioRef.collection("rutas")
                        .whereEqualTo("rutaHecha", true)
                        .get()
                        .addOnSuccessListener { documentos ->
                            listaRutas.clear()
                            for (documentoRuta in documentos) {
                                val ruta = documentoRuta.toObject(Ruta::class.java)
                                    .copy(imagenUsuario = imagenPerfil)
                                listaRutas.add(ruta)
                            }
                            rutaAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "Error obteniendo rutas hechas", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error obteniendo perfil de usuario", e)
                }
        }
    }

}
