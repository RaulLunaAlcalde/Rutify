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

class HomeDefFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var rutaAdapter: RutaHomeAdapter
    private val listaRutasPendientes = mutableListOf<Ruta>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_def, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        rutaAdapter = RutaHomeAdapter(listaRutasPendientes, "homeDef")
        recyclerView.adapter = rutaAdapter

        obtenerRutasPendientesDesdeFirebase()
    }

    private fun obtenerRutasPendientesDesdeFirebase() {
        val auth = FirebaseAuth.getInstance()
        val usuarioActual = auth.currentUser

        if (usuarioActual != null) {
            val usuarioEmail = usuarioActual.email ?: return
            val usuarioRef = FirebaseFirestore.getInstance().collection("usuarios").document(usuarioEmail)

            usuarioRef.collection("rutas")
                .whereEqualTo("rutaHecha", false) // Solo rutas NO hechas
                .get()
                .addOnSuccessListener { documentos ->
                    listaRutasPendientes.clear()
                    for (documentoRuta in documentos) {
                        val ruta = documentoRuta.toObject(Ruta::class.java)
                        listaRutasPendientes.add(ruta)
                    }
                    rutaAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error obteniendo rutas pendientes", e)
                }
        }
    }
}
