package com.rlunaalc.rutify

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

class RutaAdapter(
    private val listaRutas: List<Ruta>,
    private val origen: String // NUEVO
    ) : RecyclerView.Adapter<RutaAdapter.RutaViewHolder>() {


    class RutaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvImagenPerfil: ImageView = itemView.findViewById(R.id.tvImagenPerfil)
        val tvNombreUsuario: TextView = itemView.findViewById(R.id.tvNombreUsuario)
        val tvImagenRuta: ImageView = itemView.findViewById(R.id.tvImagenRuta)
        val tvNombreRuta: TextView = itemView.findViewById(R.id.tvNombreRuta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.publicacion_cardview, parent, false)
        return RutaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RutaViewHolder, position: Int) {
        val ruta = listaRutas[position]

        holder.tvNombreUsuario.text = ruta.usuario
        holder.tvNombreRuta.text = ruta.nombre
        holder.tvImagenPerfil.setImageResource(R.drawable.ic_android_white_24dp)
        holder.tvImagenRuta.setImageResource(R.drawable.ic_android_black_24dp)

        holder.itemView.setOnClickListener {
            val navController = Navigation.findNavController(holder.itemView)

            // Mostrar Snackbar "Iniciando ruta..."
            Snackbar.make(holder.itemView, "Iniciando ruta...", Snackbar.LENGTH_SHORT).show()

            if (origen == "historial") {
                val action = HistorialFragmentDirections.actionHistorialFragmentToRealizarRuta(ruta.nombre)
                navController.navigate(action)
            } else if (origen == "homeDef") {
                val action = HomeDefFragmentDirections.actionHomeDefFragmentToRealizarRuta(ruta.nombre)
                navController.navigate(action)
            } else if (origen == "retos") {
                val action = RetosFragmentDirections.actionRetosFragmentToRealizarRuta(ruta.nombre)
                navController.navigate(action)
            }
        }
    }


    override fun getItemCount(): Int = listaRutas.size
}
