package com.rlunaalc.rutify

import android.annotation.SuppressLint
import android.app.Dialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade


class RutaHomeAdapter(
    private val listaRutas: List<Ruta>,
    private val origen: String // NUEVO
    ) : RecyclerView.Adapter<RutaHomeAdapter.RutaViewHolder>() {


    class RutaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvImagenPerfil: ImageView = itemView.findViewById(R.id.tvImagenPerfil)
        val tvNombreRuta: TextView = itemView.findViewById(R.id.tvNombreRuta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.publicacion_cardview_home_sin_foto, parent, false)
        return RutaViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RutaViewHolder, position: Int) {
        val ruta = listaRutas[position]

        holder.tvNombreRuta.text = ruta.nombre
        if (!ruta.imagenUsuario.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(ruta.imagenUsuario)
                .placeholder(R.drawable.ic_android_white_24dp)
                .circleCrop()
                .transition(withCrossFade())
                .into(holder.tvImagenPerfil)
        } else {
            holder.tvImagenPerfil.setImageResource(R.drawable.ic_android_white_24dp)
        }


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
