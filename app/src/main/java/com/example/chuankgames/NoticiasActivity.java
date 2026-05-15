package com.example.chuankgames;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NoticiasActivity extends AppCompatActivity {

    private RecyclerView recyclerNoticias;
    private NoticiaAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvError;

    private final List<Noticia> listaNoticias = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_noticias);

        recyclerNoticias = findViewById(R.id.recyclerNoticias);
        progressBar      = findViewById(R.id.progressBarNoticias);
        tvError          = findViewById(R.id.tvErrorNoticias);

        View btnVolver = findViewById(R.id.btnVolverNoticias);
        btnVolver.setOnClickListener(v -> finish());

        adapter = new NoticiaAdapter(listaNoticias, noticia -> {
            if (noticia.getUrl() != null && !noticia.getUrl().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(noticia.getUrl()));
                startActivity(intent);
            }
        });
        recyclerNoticias.setLayoutManager(new LinearLayoutManager(this));
        recyclerNoticias.setAdapter(adapter);

        cargarNoticias();
    }

    private void cargarNoticias() {
        progressBar.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GamerPowerApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GamerPowerApi api = retrofit.create(GamerPowerApi.class);
        api.obtenerNoticias().enqueue(new Callback<List<Noticia>>() {
            @Override
            public void onResponse(Call<List<Noticia>> call, Response<List<Noticia>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    listaNoticias.clear();
                    listaNoticias.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    if (listaNoticias.isEmpty()) {
                        tvError.setText("No hay noticias disponibles ahora mismo.");
                        tvError.setVisibility(View.VISIBLE);
                    }
                } else {
                    mostrarError("Error al obtener noticias.");
                }
            }

            @Override
            public void onFailure(Call<List<Noticia>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                mostrarError("Sin conexión. Comprueba tu internet.");
            }
        });
    }

    private void mostrarError(String mensaje) {
        tvError.setText(mensaje);
        tvError.setVisibility(View.VISIBLE);
    }
}
