package com.example.chuankgames;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.List;

public class VideojuegoAdapter extends RecyclerView.Adapter<VideojuegoAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Videojuego videojuego);
    }

    private final List<Videojuego> lista;
    private final OnItemClickListener listener;

    public VideojuegoAdapter(List<Videojuego> lista, OnItemClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_videojuego, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Videojuego juego = lista.get(position);

        holder.tvNombre.setText(juego.getNombre());
        holder.tvGenero.setText(juego.getGenero());
        holder.tvDescripcion.setText(juego.getDescripcion());
        holder.tvPrecio.setText(String.format("%.2f€", juego.getPrecio()));

        // Cargar imagen desde Firebase Storage con Glide
        if (juego.getImagenUrl() != null && !juego.getImagenUrl().isEmpty()) {
            Glide.with(holder.imgJuego.getContext())
                    .load(juego.getImagenUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(holder.imgJuego);
        } else {
            holder.imgJuego.setImageResource(R.mipmap.ic_launcher);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(juego));
    }

    @Override
    public int getItemCount() { return lista.size(); }

    public void actualizarLista(List<Videojuego> nuevaLista) {
        lista.clear();
        lista.addAll(nuevaLista);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgJuego;
        TextView tvNombre, tvGenero, tvDescripcion, tvPrecio;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgJuego      = itemView.findViewById(R.id.imgJuego);
            tvNombre      = itemView.findViewById(R.id.tvNombreJuego);
            tvGenero      = itemView.findViewById(R.id.tvGeneroJuego);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionJuego);
            tvPrecio      = itemView.findViewById(R.id.tvPrecioJuego);
        }
    }
}