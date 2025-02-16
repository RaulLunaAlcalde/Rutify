package com.rlunaalc.rutify.ui.register

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.rlunaalc.rutify.R
import com.rlunaalc.rutify.databinding.FragmentLoginBinding
import com.rlunaalc.rutify.databinding.FragmentRegisterBinding


class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private lateinit var firebaseAuth : FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        firebaseAuth = FirebaseAuth.getInstance()


        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val pass = binding.passwordInput.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()){
                firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener{
                    if (it.isSuccessful){
                        Toast.makeText(requireContext(), "User created successfully!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.nav_home)
                    }else {
                        Toast.makeText(requireContext(), it.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
            } else{
                Toast.makeText(requireContext(), "Required files are incomplete!", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

}