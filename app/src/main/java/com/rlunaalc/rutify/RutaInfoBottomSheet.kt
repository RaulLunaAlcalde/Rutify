package com.rlunaalc.rutify.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rlunaalc.rutify.databinding.BottomSheetRutaInfoBinding

class RutaInfoBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRutaInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRutaInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Método para actualizar la información de la ruta
    fun actualizarInformacion(info: String) {
        binding.rutaInfoText.text = info
    }
}
