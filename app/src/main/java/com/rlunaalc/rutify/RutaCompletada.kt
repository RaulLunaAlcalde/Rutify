package com.rlunaalc.rutify.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.rlunaalc.rutify.R
import java.util.concurrent.TimeUnit

class RutaCompletada : Fragment() {

    private val args: RutaCompletadaArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ruta_completada, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val popup = view.findViewById<View>(R.id.popupRutaCompletada)
        popup.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(300)
            .start()

        val duracion = formatTiempo(args.duracionSegundos)
        view.findViewById<TextView>(R.id.tvDuracionFinal).text = "Duración: $duracion"
        view.findViewById<TextView>(R.id.tvDistanciaFinal).text = "Distancia: %.2f km".format(args.distanciaKm)
        view.findViewById<TextView>(R.id.tvRitmoFinal).text = "Ritmo: %.2f min/km".format(args.ritmoMinKm)
        view.findViewById<TextView>(R.id.tvKcalFinal).text = "Calorías: %.0f kcal".format(args.kcal)
    }

    private fun formatTiempo(segundos: Int): String {
        val minutos = TimeUnit.SECONDS.toMinutes(segundos.toLong())
        val segundosRestantes = segundos % 60
        return "%02d:%02d".format(minutos, segundosRestantes)
    }
}
