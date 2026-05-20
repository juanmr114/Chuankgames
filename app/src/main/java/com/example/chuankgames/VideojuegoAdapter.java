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
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class VideojuegoAdapter extends RecyclerView.Adapter<VideojuegoAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Videojuego videojuego);
    }

    public interface OnCodigoClickListener {
        void onCodigoClick(Videojuego videojuego);
    }

    private final List<Videojuego>  lista;
    private final OnItemClickListener listener;
    private OnCodigoClickListener codigoListener;

    public VideojuegoAdapter(List<Videojuego> lista, OnItemClickListener listener) {
        this.lista    = lista;
        this.listener = listener;
    }

    public void setOnCodigoClickListener(OnCodigoClickListener l) {
        this.codigoListener = l;
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
        holder.tvPrecio.setText(String.format("💶 €%.2f", juego.getPrecioEuros()));

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

        // Botón "Ver código" — visible solo en la biblioteca
        if (codigoListener != null) {
            holder.btnCodigo.setVisibility(View.VISIBLE);
            holder.btnCodigo.setOnClickListener(v -> codigoListener.onCodigoClick(juego));
        } else {
            holder.btnCodigo.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return lista != null ? lista.size() : 0;
    }

    public void actualizarLista(List<Videojuego> nuevaLista) {
        if (this.lista != nuevaLista) {
            this.lista.clear();
            this.lista.addAll(nuevaLista);
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView      imgJuego;
        TextView       tvNombre, tvGenero, tvDescripcion, tvPrecio;
        MaterialButton btnCodigo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgJuego      = itemView.findViewById(R.id.imgJuego);
            tvNombre      = itemView.findViewById(R.id.tvNombreJuego);
            tvGenero      = itemView.findViewById(R.id.tvGeneroJuego);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionJuego);
            tvPrecio      = itemView.findViewById(R.id.tvPrecioJuego);
            btnCodigo     = itemView.findViewById(R.id.btnVerCodigo);
        }
    }
}
