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

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RutaViewHolder, position: Int) {
        val ruta = listaRutas[position]

        holder.tvNombreUsuario.text = ruta.usuario
        holder.tvNombreRuta.text = ruta.nombre
        holder.tvImagenPerfil.setImageResource(R.drawable.ic_android_white_24dp)
        holder.tvImagenRuta.setImageResource(R.drawable.ic_android_black_24dp)

        if (!ruta.imagenRuta.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(ruta.imagenRuta)
                .placeholder(R.drawable.ic_android_black_24dp)
                .transition(withCrossFade()) // 🔥 Esta línea añade el efecto de transición suave
                .into(holder.tvImagenRuta)

        } else {
            holder.tvImagenRuta.setImageResource(R.drawable.ic_android_black_24dp)
        }

        holder.tvImagenRuta.setOnClickListener {
            if (!ruta.imagenRuta.isNullOrEmpty()) {
                val dialog = Dialog(holder.itemView.context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                dialog.setContentView(R.layout.dialog_imagen_grande)
                val imageView = dialog.findViewById<ImageView>(R.id.dialogImageView)

                Glide.with(holder.itemView.context)
                    .load(ruta.imagenRuta)
                    .transition(withCrossFade())
                    .into(imageView)

                // 🚀 Detectar swipe hacia abajo para cerrar
                var yStart = 0f
                imageView.setOnTouchListener { v, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            yStart = event.y
                            true
                        }
                        android.view.MotionEvent.ACTION_UP -> {
                            val yEnd = event.y
                            if (yEnd - yStart > 300) { // 300px de movimiento hacia abajo
                                dialog.dismiss()
                            }
                            true
                        }
                        else -> false
                    }
                }

                dialog.show()
            }
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
