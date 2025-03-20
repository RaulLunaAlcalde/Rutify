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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.rlunaalc.rutify.R
import com.rlunaalc.rutify.Ruta
import com.rlunaalc.rutify.RutaAdapter

class HistorialFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var rutaAdapter: RutaAdapter
    private val db = FirebaseFirestore.getInstance()
    private val listaRutas = mutableListOf<Ruta>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_historial, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        rutaAdapter = RutaAdapter(listaRutas)
        recyclerView.adapter = rutaAdapter

        obtenerRutasDesdeFirebase()
    }

    private fun obtenerRutasDesdeFirebase() {
        val auth = FirebaseAuth.getInstance()
        val usuarioActual = auth.currentUser

        if (usuarioActual != null) {
            val usuarioEmail = usuarioActual.email ?: run {
                Log.e("FirebaseAuth", "El usuario no tiene email")
                return
            }

            val usuarioSanitizado = usuarioEmail.replace(".", "_") // Sanitizamos el email

            Log.d("Firebase", "Correo del usuario: $usuarioEmail")
            Log.d("Firebase", "Correo sanitizado: $usuarioSanitizado")

            val usuarioRef = db.collection("usuarios").document(usuarioEmail)

            // 1. Obtener el nombre del usuario
            usuarioRef.get()
                .addOnSuccessListener { documento ->
                    if (documento.exists()) {
                        val nombreUsuario = documento.getString("nombre") ?: "Desconocido"

                        // 2. Obtener las rutas del usuario
                        usuarioRef.collection("rutas").get()
                            .addOnSuccessListener { documentos ->
                                listaRutas.clear() // Limpiamos la lista antes de llenarla
                                for (documentoRuta in documentos) {
                                    val nombreRuta = documentoRuta.id // El ID es el nombre de la ruta

                                    // Crear el objeto Ruta
                                    val ruta = Ruta(
                                        nombre = nombreRuta,
                                        usuario = nombreUsuario,
                                        imagenRuta = "", // Deberías agregar lógica para obtener la imagen
                                        imagenUsuario = "", // Deberías agregar lógica para obtener la imagen del usuario
                                        coordenadas = emptyList() // Aquí deberías agregar las coordenadas si las tienes
                                    )

                                    Log.d("Firebase", "Ruta obtenida: $nombreRuta, Usuario: $nombreUsuario")
                                    listaRutas.add(ruta)
                                }
                                rutaAdapter.notifyDataSetChanged() // Notificar al adapter que la lista ha cambiado
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firebase", "Error obteniendo rutas", e)
                            }
                    } else {
                        Log.e("Firebase", "No se encontró el documento del usuario")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error obteniendo usuario", e)
                }
        } else {
            Log.e("Firebase", "No hay usuario autenticado")
        }
    }

}