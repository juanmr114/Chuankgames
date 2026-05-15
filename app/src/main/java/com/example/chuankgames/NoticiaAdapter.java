package com.example.chuankgames;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class NoticiaAdapter extends RecyclerView.Adapter<NoticiaAdapter.ViewHolder> {

    public interface OnNoticiaClickListener {
        void onClick(Noticia noticia);
    }

    private final List<Noticia> lista;
    private final OnNoticiaClickListener listener;

    public NoticiaAdapter(List<Noticia> lista, OnNoticiaClickListener listener) {
        this.lista    = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_noticia, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Noticia noticia = lista.get(position);
        holder.tvTitulo.setText(noticia.getTitulo());
        holder.tvDescripcion.setText(noticia.getDescripcion());
        holder.tvFuente.setText(noticia.getFuente());

        if (noticia.getUrlImagen() != null && !noticia.getUrlImagen().isEmpty()) {
            Glide.with(holder.ivImagen.getContext())
                    .load(noticia.getUrlImagen())
                    .centerCrop()
                    .placeholder(R.mipmap.ic_launcher)
                    .into(holder.ivImagen);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(noticia));
    }

    @Override
    public int getItemCount() { return lista.size(); }

    public void setLista(List<Noticia> nuevaLista) {
        lista.clear();
        lista.addAll(nuevaLista);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImagen;
        TextView tvTitulo, tvDescripcion, tvFuente;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImagen      = itemView.findViewById(R.id.ivNoticiaImagen);
            tvTitulo      = itemView.findViewById(R.id.tvNoticiaTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvNoticiaDescripcion);
            tvFuente      = itemView.findViewById(R.id.tvNoticiaFuente);
        }
    }
}