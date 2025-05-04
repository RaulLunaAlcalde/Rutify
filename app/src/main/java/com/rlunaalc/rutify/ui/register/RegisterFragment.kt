package com.rlunaalc.rutify.ui.register

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.TextView
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
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

            if (email.isNotEmpty() && pass.isNotEmpty() && username.isNotEmpty()) {
                firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        guardarUsuarioEnFirestore(email, username, pass)
                    } else {
                        showSnackbar(authTask.exception?.message ?: "Error desconocido")
                    }
                }
            } else {
                showSnackbar("¡Todos los campos son obligatorios!")
            }
        }

        return binding.root
    }

    private fun guardarUsuarioEnFirestore(email: String, username: String, pass: String) {
        val usuariosRef = firestore.collection("usuarios")

        usuariosRef.get().addOnSuccessListener { snapshot ->
            var maxId = 0L
            for (document in snapshot) {
                val id = document.getLong("id")
                if (id != null && id > maxId) {
                    maxId = id
                }
            }

            val nuevoId = maxId + 1

            val user = mapOf(
                "email" to email,
                "usuario" to username,
                "contrasenya" to pass,
                "fechaRegistro" to com.google.firebase.Timestamp.now(),
                "id" to nuevoId
            )

            usuariosRef.document(email)
                .set(user)
                .addOnSuccessListener {
                    showSnackbar("¡Usuario registrado correctamente!")
                    findNavController().navigate(R.id.nav_home)
                }
                .addOnFailureListener {
                    showSnackbar("Error al guardar en Firestore")
                }

        }.addOnFailureListener {
            showSnackbar("Error al generar ID de usuario")
        }
    }


    private fun showSnackbar(message: String) {
        val snackbar = com.google.android.material.snackbar.Snackbar.make(requireView(), message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
        snackbar.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Animaciones de entrada
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)

        view.findViewById<EditText>(R.id.email_input).startAnimation(slideUp)
        view.findViewById<EditText>(R.id.password_input).startAnimation(slideUp)
        view.findViewById<EditText>(R.id.username_input).startAnimation(slideUp)
        view.findViewById<Button>(R.id.login_button).startAnimation(slideUp)
        view.findViewById<LinearLayout>(R.id.legal_links).startAnimation(slideUp)

        // Máquina de escribir efecto
        setupTypingEffect(view)
    }

    private fun setupTypingEffect(view: View) {
        val titleTextView = view.findViewById<TextView>(R.id.title)
        val fullText = "Inscriu-te a\nRutify"
        val typingDelay: Long = 80
        val cursorBlinkDelay: Long = 500

        var index = 0
        var repeatCount = 0
        val maxRepeats = 3
        var cursorVisible = true
        val handler = Handler(Looper.getMainLooper())

        lateinit var cursorBlinkRunnable: Runnable

        val typingRunnable = object : Runnable {
            override fun run() {
                if (index <= fullText.length) {
                    val text = fullText.substring(0, index)
                    titleTextView.text = if (cursorVisible) "$text|" else "$text"
                    cursorVisible = !cursorVisible
                    index++
                    handler.postDelayed(this, typingDelay)
                } else {
                    repeatCount++
                    if (repeatCount < maxRepeats) {
                        handler.postDelayed({
                            index = 0
                            titleTextView.text = ""
                            handler.post(this)
                        }, 500)
                    } else {
                        handler.post(cursorBlinkRunnable)
                    }
                }
            }
        }

        cursorBlinkRunnable = object : Runnable {
            override fun run() {
                val text = fullText
                titleTextView.text = if (cursorVisible) "$text|" else text
                cursorVisible = !cursorVisible
                handler.postDelayed(this, cursorBlinkDelay)
            }
        }

        handler.post(typingRunnable)
    }
}
