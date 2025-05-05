package com.rlunaalc.rutify.ui.login

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.rlunaalc.rutify.R
import com.rlunaalc.rutify.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private lateinit var firebaseAuth : FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        firebaseAuth = FirebaseAuth.getInstance()

        binding.registerButton.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val pass = binding.passwordInput.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()){
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener{
                    if (it.isSuccessful){
                        Toast.makeText(requireContext(), "Log in successfully!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.nav_home)
                    }else {
                        Toast.makeText(requireContext(), it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            } else{
                Toast.makeText(requireContext(), "Required files are incomplete!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.registerButton.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleTextView = view.findViewById<TextView>(R.id.title)

        val fullText = "Uneix-te a\nRutify"
        val typingDelay: Long = 80
        val cursorBlinkDelay: Long = 500
        val pauseBeforeRestart: Long = 10000
        val maxRepeats = 3

        var index = 0
        var repeatCount = 0
        var cursorVisible = true

        val handler = Handler(Looper.getMainLooper())

        lateinit var cursorBlinkRunnable: Runnable
        lateinit var typingRunnable: Runnable

        typingRunnable = object : Runnable {
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
                            handler.post(typingRunnable)
                        }, pauseBeforeRestart)
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

        // Animaciones para el formulario
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)

        view.findViewById<EditText>(R.id.email_input).startAnimation(slideUp)
        view.findViewById<EditText>(R.id.password_input).startAnimation(slideUp)
        view.findViewById<Button>(R.id.login_button).startAnimation(slideUp)
        view.findViewById<Button>(R.id.register_button).startAnimation(slideUp)
        view.findViewById<TextView>(R.id.or_separator).startAnimation(slideUp)
        view.findViewById<LinearLayout>(R.id.legal_links).startAnimation(slideUp)
    }





}