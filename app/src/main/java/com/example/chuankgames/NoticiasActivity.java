package com.example.chuankgames;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoticiasActivity extends AppCompatActivity {

    private static final String API_URL =
            "https://content.guardianapis.com/search?q=videogames+gaming" +
            "&section=games&api-key=test&show-fields=thumbnail,trailText" +
            "&page-size=20&order-by=newest";

    // Caché estático para no golpear la API en cada visita
    private static List<Noticia> cachedNoticias   = null;
    private static long          cacheTimestamp   = 0;
    private static final long    CACHE_TTL_MS     = 5 * 60 * 1000; // 5 minutos

    private RecyclerView    recyclerNoticias;
    private NoticiaAdapter  adapter;
    private ProgressBar     progressBar;
    private TextView        tvError;
    private LinearLayout    layoutError;
    private MaterialButton  btnReintentar;

    private final List<Noticia>    noticias = new ArrayList<>();
    private final ExecutorService  executor = Executors.newSingleThreadExecutor();
    private final Handler          handler  = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_noticias);

        recyclerNoticias = findViewById(R.id.recyclerNoticias);
        progressBar      = findViewById(R.id.progressNoticias);
        tvError          = findViewById(R.id.tvErrorNoticias);
        layoutError      = findViewById(R.id.layoutError);
        btnReintentar    = findViewById(R.id.btnReintentar);

        adapter = new NoticiaAdapter(noticias, noticia -> {
            if (noticia.getUrl() != null && !noticia.getUrl().isEmpty()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(noticia.getUrl())));
            }
        });

        recyclerNoticias.setLayoutManager(new LinearLayoutManager(this));
        recyclerNoticias.setAdapter(adapter);

        btnReintentar.setOnClickListener(v -> {
            cachedNoticias = null;   // forzar recarga
            cargarNoticias();
        });

        configurarBottomNav();
        cargarNoticias();
    }

    private void configurarBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_buscar);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                Intent i = new Intent(this, PaginaPrincipalActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                return true;
            } else if (id == R.id.nav_buscar) {
                return true;
            } else if (id == R.id.nav_añadir) {
                startActivity(new Intent(this, PublicarJuegoActivity.class));
                return true;
            } else if (id == R.id.nav_carrito) {
                startActivity(new Intent(this, BibliotecaActivity.class));
                return true;
            } else if (id == R.id.nav_ruleta) {
                startActivity(new Intent(this, RuletaActivity.class));
                return true;
            }
            return false;
        });
    }

    private void cargarNoticias() {
        // Si tenemos caché fresca, usarla directamente
        if (cachedNoticias != null &&
                System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS) {
            mostrarNoticias(cachedNoticias);
            return;
        }

        mostrarEstado(true, false, "");

        executor.execute(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "ChuankgamesApp/1.0");

                int code = conn.getResponseCode();

                if (code == 429) {
                    throw new Exception("⚠️ Demasiadas peticiones a la API.\n" +
                            "Espera un momento y pulsa Reintentar.");
                }
                if (code != 200) {
                    throw new Exception("Error del servidor: " + code);
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json   = new JSONObject(sb.toString());
                JSONArray results = json.getJSONObject("response").getJSONArray("results");
                List<Noticia> resultado = new ArrayList<>();

                for (int i = 0; i < results.length(); i++) {
                    JSONObject art    = results.getJSONObject(i);
                    JSONObject fields = art.optJSONObject("fields");

                    String titulo      = art.optString("webTitle", "Sin título");
                    String descripcion = fields != null ? fields.optString("trailText", "") : "";
                    descripcion        = descripcion.replaceAll("<[^>]*>", "").trim();
                    String urlImagen   = fields != null ? fields.optString("thumbnail", "") : "";
                    String urlArt      = art.optString("webUrl", "");
                    String fecha       = art.optString("webPublicationDate", "");

                    if (!titulo.isEmpty() && !descripcion.isEmpty()) {
                        resultado.add(new Noticia(titulo, descripcion, urlImagen,
                                urlArt, "The Guardian", fecha));
                    }
                }

                // Guardar en caché
                cachedNoticias = resultado;
                cacheTimestamp = System.currentTimeMillis();

                handler.post(() -> {
                    if (resultado.isEmpty()) {
                        mostrarEstado(false, true, "No se encontraron noticias.");
                    } else {
                        mostrarNoticias(resultado);
                    }
                });

            } catch (Exception e) {
                handler.post(() ->
                        mostrarEstado(false, true, e.getMessage()));
            }
        });
    }

    private void mostrarNoticias(List<Noticia> lista) {
        mostrarEstado(false, false, "");
        adapter.setLista(lista);
    }

    private void mostrarEstado(boolean cargando, boolean error, String msg) {
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        layoutError.setVisibility(error    ? View.VISIBLE : View.GONE);
        recyclerNoticias.setVisibility(!cargando && !error ? View.VISIBLE : View.GONE);
        if (error) tvError.setText(msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
