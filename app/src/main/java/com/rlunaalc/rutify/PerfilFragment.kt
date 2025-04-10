package com.rlunaalc.rutify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rlunaalc.rutify.databinding.FragmentPerfilBinding
import com.bumptech.glide.Glide

class PerfilFragment : Fragment() {
    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cargarDatosPerfil()

        binding.btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }
    }

    private fun cargarDatosPerfil() {
        val usuarioActual = auth.currentUser
        if (usuarioActual != null) {
            val usuarioEmail = usuarioActual.email ?: run {
                Log.e("FirebaseAuth", "El usuario no tiene email")
                return
            }

            val usuarioRef = db.collection("usuarios").document(usuarioEmail)

            usuarioRef.get()
                .addOnSuccessListener { documento ->
                    if (documento.exists()) {
                        val usuarioId = documento.getLong("id") ?: run {
                            Log.e("Perfil", "No se encontró el ID del usuario")
                            return@addOnSuccessListener
                        }
                        val nombre = documento.getString("nombre") ?: "Usuario"
                        val imagenPerfil = documento.getString("imagenPerfil") ?: ""

                        binding.txtNombre.text = nombre

                        if (imagenPerfil.isNotEmpty()) {
                            Glide.with(this).load(imagenPerfil).into(binding.imgPerfil)
                        }

                        obtenerSeguidores(usuarioId)
                        obtenerSeguidos(usuarioId)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Perfil", "Error obteniendo datos del usuario", e)
                }
        }
    }


    private fun obtenerSeguidores(usuarioId: Long) {
        db.collection("seguidores")
            .whereEqualTo("id_seguido", usuarioId)
            .get()
            .addOnSuccessListener { documents ->
                binding.txtSeguidores.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("Perfil", "Error obteniendo seguidores", e)
            }
    }

    private fun obtenerSeguidos(usuarioId: Long) {
        db.collection("seguidores")
            .whereEqualTo("id_usuario", usuarioId)
            .get()
            .addOnSuccessListener { documents ->
                binding.txtSiguiendo.text = documents.size().toString()
            }
            .addOnFailureListener { e ->
                Log.e("Perfil", "Error obteniendo seguidos", e)
            }
    }

    private fun cerrarSesion() {
        auth.signOut()

        val navController = findNavController()
        navController.navigate(R.id.loginFragment)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
