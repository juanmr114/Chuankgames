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

public class NoticiaAdapter extends RecyclerView.Adapter<NoticiaAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Noticia noticia);
    }

    private final List<Noticia> lista;
    private final OnItemClickListener listener;

    public NoticiaAdapter(List<Noticia> lista, OnItemClickListener listener) {
        this.lista = lista;
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
        holder.tvPlataforma.setText(noticia.getPlataformas() != null ? noticia.getPlataformas() : "");

        String valor = noticia.getValor();
        if (valor != null && !valor.isEmpty() && !valor.equals("N/A")) {
            holder.tvValor.setText("Gratis · Valor: " + valor);
            holder.tvValor.setVisibility(View.VISIBLE);
        } else {
            holder.tvValor.setVisibility(View.GONE);
        }

        if (noticia.getThumbnailUrl() != null && !noticia.getThumbnailUrl().isEmpty()) {
            Glide.with(holder.imgPortada.getContext())
                    .load(noticia.getThumbnailUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(holder.imgPortada);
        } else {
            holder.imgPortada.setImageResource(R.mipmap.ic_launcher);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(noticia));
    }

    @Override
    public int getItemCount() {
        return lista != null ? lista.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPortada;
        TextView tvTitulo, tvDescripcion, tvPlataforma, tvValor;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPortada    = itemView.findViewById(R.id.imgPortadaNoticia);
            tvTitulo      = itemView.findViewById(R.id.tvTituloNoticia);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionNoticia);
            tvPlataforma  = itemView.findViewById(R.id.tvPlataformaNoticia);
            tvValor       = itemView.findViewById(R.id.tvValorNoticia);
        }
    }
}
