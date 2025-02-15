package com.rlunaalc.rutify

import android.app.ProgressDialog
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rlunaalc.rutify.databinding.FragmentLoginBinding
import kotlin.properties.Delegates

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private var email by Delegates.notNull<String>()
    private var password by Delegates.notNull<String>()
    private lateinit var email_input: EditText
    private lateinit var password_input: EditText
    private lateinit var mProgressBar: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        initialise()

        binding.loginButton.setOnClickListener {
            loginUser()
        }

        return binding.root
    }

    private fun initialise() {
        email_input = binding.emailInput
        password_input = binding.passwordInput
        mProgressBar = ProgressDialog(requireContext())
        auth = FirebaseAuth.getInstance()
    }

    private fun loginUser() {
        email = email_input.text?.toString() ?: ""
        password = password_input.text?.toString() ?: ""

        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            db.collection("usuaris").document(userId).get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        val userData = document.data
                                        Toast.makeText(requireContext(), "Login exitoso", Toast.LENGTH_SHORT).show()
                                        // Aquí puedes guardar los datos del usuario o navegar a otra pantalla
                                    } else {
                                        Toast.makeText(requireContext(), "Usuario no encontrado en Firestore", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(requireContext(), "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        val errorMessage = task.exception?.message ?: "Authentication failed."
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(requireContext(), "Enter all details", Toast.LENGTH_SHORT).show()
        }
    }
}