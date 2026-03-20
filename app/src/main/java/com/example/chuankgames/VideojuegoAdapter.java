package com.example.chuankgames;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class VideojuegoAdapter extends RecyclerView.Adapter<VideojuegoAdapter.ViewHolder> {

    private List<Videojuego> lista;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Videojuego videojuego);
    }

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
        holder.tvPrecio.setText(String.format(Locale.getDefault(), "%.2f€", juego.getPrecio()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(juego);
        });
    }

    @Override
    public int getItemCount() {
        return lista != null ? lista.size() : 0;
    }

    public void actualizarLista(List<Videojuego> nuevaLista) {
        this.lista = nuevaLista;
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
