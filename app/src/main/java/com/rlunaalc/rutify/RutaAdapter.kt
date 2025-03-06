package com.rlunaalc.rutify

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RutaAdapter(private val listaRutas: List<Ruta>) :
    RecyclerView.Adapter<RutaAdapter.RutaViewHolder>() {

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

        Log.d("RecyclerView", "Nombre usuario: ${ruta.usuario}, Nombre ruta: ${ruta.nombre}")

        holder.tvNombreUsuario.text = ruta.usuario
        holder.tvNombreRuta.text = ruta.nombre

        holder.tvImagenPerfil.setImageResource(R.drawable.ic_android_white_24dp)
        holder.tvImagenRuta.setImageResource(R.drawable.ic_android_black_24dp)
    }

    override fun getItemCount(): Int = listaRutas.size
}
