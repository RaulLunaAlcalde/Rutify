package com.rlunaalc.rutify

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.rlunaalc.rutify.ui.home.RealizarRuta


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

    // RutaAdapter.kt
    override fun onBindViewHolder(holder: RutaViewHolder, position: Int) {
        val ruta = listaRutas[position]

        holder.tvNombreUsuario.text = ruta.usuario
        holder.tvNombreRuta.text = ruta.nombre

        holder.tvImagenPerfil.setImageResource(R.drawable.ic_android_white_24dp)
        holder.tvImagenRuta.setImageResource(R.drawable.ic_android_black_24dp)

        holder.itemView.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("rutaName", ruta.nombre)

            val realizarRutaFragment = RealizarRuta()
            realizarRutaFragment.arguments = bundle

            // Acceder al contexto a través de itemView y obtener la actividad asociada
            val fragmentManager = (holder.itemView.context as AppCompatActivity).supportFragmentManager

            // Asegurarse de que el contenedor de fragmentos esté disponible (puedes cambiar 'fragment_container' por el ID correcto de tu layout)
            fragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_content_main, realizarRutaFragment)
                .addToBackStack(null)
                .commit()
        }
    }


    override fun getItemCount(): Int = listaRutas.size
}
