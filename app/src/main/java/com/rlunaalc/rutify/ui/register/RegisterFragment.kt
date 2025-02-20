package com.rlunaalc.rutify.ui.register

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rlunaalc.rutify.R
import com.rlunaalc.rutify.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val pass = binding.passwordInput.text.toString().trim()
            val username = binding.usernameInput.text.toString().trim()
            val name = binding.nameInput.text.toString().trim()
            val lastName = binding.lastnameInput.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty() && username.isNotEmpty() && name.isNotEmpty() && lastName.isNotEmpty()) {
                firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        guardarUsuarioEnFirestore(email, username, name, lastName, pass)
                    } else {
                        Toast.makeText(requireContext(), authTask.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "¡Todos los campos son obligatorios!", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun guardarUsuarioEnFirestore(email: String, username: String, name: String, lastName: String, pass: String) {
        val user = mapOf(
            "email" to email,
            "usuario" to username,
            "nombre" to name,
            "apellidos" to lastName,
            "contrasenya" to pass,
            "fechaRegistro" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("usuarios").document(email)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Usuario registrado correctamente!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.nav_home)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al guardar en Firestore", Toast.LENGTH_SHORT).show()
            }
    }
}
