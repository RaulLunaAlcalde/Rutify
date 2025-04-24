package com.rlunaalc.rutify

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rlunaalc.rutify.databinding.FragmentPerfilBinding
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class PerfilFragment : Fragment() {
    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val supabaseUrl = "https://lsowlungekgzqifulsoh.supabase.co"
    private val supabaseApiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imxzb3dsdW5nZWtnenFpZnVsc29oIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mzg4MjY0OTcsImV4cCI6MjA1NDQwMjQ5N30.Z5xwaxpHFh6U5F_Z-nMjNTh4vzc5KHYvah8pkciFJeo"
    private val bucketName = "foto"

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch {
                uploadImageToSupabase(requireContext(), it)
            }
        }
    }

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

        // Al hacer clic en la imagen de perfil, se selecciona una nueva imagen
        binding.imgPerfil.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun cargarDatosPerfil() {
        val usuarioActual = auth.currentUser
        if (usuarioActual != null) {
            val usuarioEmail = usuarioActual.email ?: return

            val usuarioRef = db.collection("usuarios").document(usuarioEmail)

            usuarioRef.get()
                .addOnSuccessListener { documento ->
                    if (documento.exists()) {
                        val usuarioId = documento.getLong("id") ?: return@addOnSuccessListener
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

    private suspend fun uploadImageToSupabase(context: Context, uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val fileBytes = inputStream?.readBytes() ?: return@withContext
                val fileName = "perfil_${System.currentTimeMillis()}.jpg"

                val client = HttpClient(Android) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }

                val response = client.post("$supabaseUrl/storage/v1/object/$bucketName/$fileName") {
                    header("Authorization", "Bearer $supabaseApiKey")
                    header("apikey", supabaseApiKey)
                    header(HttpHeaders.ContentType, "image/jpeg")
                    header(HttpHeaders.ContentDisposition, "inline; filename=\"$fileName\"")
                    parameter("upsert", "true")
                    setBody(fileBytes)
                }

                if (response.status.isSuccess()) {
                    val imageUrl = "$supabaseUrl/storage/v1/object/public/$bucketName/$fileName"
                    Log.i("Supabase", "Imagen subida correctamente: $imageUrl")
                    guardarUrlImagenEnFirestore(imageUrl)
                    withContext(Dispatchers.Main) {
                        Glide.with(this@PerfilFragment).load(imageUrl).into(binding.imgPerfil)
                    }
                } else {
                    Log.e("Supabase", "Error subiendo imagen: ${response.status}")
                }

            } catch (e: Exception) {
                Log.e("Supabase", "Excepción al subir imagen", e)
            }
        }
    }

    private fun guardarUrlImagenEnFirestore(url: String) {
        val usuarioEmail = auth.currentUser?.email ?: return
        db.collection("usuarios").document(usuarioEmail)
            .update("imagenPerfil", url)
            .addOnSuccessListener {
                Log.i("Firestore", "URL de imagen actualizada")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error actualizando imagen", e)
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
        findNavController().navigate(R.id.loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
