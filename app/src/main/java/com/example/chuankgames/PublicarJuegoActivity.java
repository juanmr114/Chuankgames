package com.example.chuankgames;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class PublicarJuegoActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://chuankgames-default-rtdb.europe-west1.firebasedatabase.app";

    // API gratuita sin clave — base de datos de precios de Steam
    private static final String CHEAPSHARK_URL =
            "https://www.cheapshark.com/api/1.0/games?title=%s&limit=1";
    private static final String STEAM_HEADER_URL =
            "https://cdn.akamai.steamstatic.com/steam/apps/%s/header.jpg";

    private static final String[] PLATAFORMAS = {
            "Steam", "Epic Games", "GOG", "Xbox", "PlayStation",
            "Nintendo Switch", "Ubisoft Connect", "Battle.net", "EA App", "Otro"
    };

    private static final String[] GENEROS = {
            "Acción", "Aventura", "RPG", "Deportes", "Simulación",
            "Estrategia", "Puzzle", "Terror", "Multijugador", "Sandbox",
            "Plataformas", "Carreras", "Lucha", "Shooter", "Indie"
    };

    // Views
    private ImageView         ivPreview;
    private TextInputEditText etUrl, etNombre, etDescripcion, etPrecio, etCodigo;
    private TextInputLayout   tilCodigo;
    private Spinner           spinnerGenero, spinnerPlataforma;
    private MaterialButton    btnBuscarPortada, btnPrevisualizar, btnPublicar;
    private ProgressBar       progressBar;
    private TextView          tvEstadoBusqueda;

    private String plataformaSeleccionada = PLATAFORMAS[0];
    private String generoSeleccionado     = GENEROS[0];
    private boolean buscando = false;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private DatabaseReference dbJuegos, dbUsuarios;
    private FirebaseUser      usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publicar_juego);

        usuario    = FirebaseAuth.getInstance().getCurrentUser();
        dbJuegos   = FirebaseDatabase.getInstance(DB_URL).getReference("videojuegos");
        dbUsuarios = FirebaseDatabase.getInstance(DB_URL).getReference("usuarios");

        ivPreview        = findViewById(R.id.ivPreviewPortada);
        etUrl            = findViewById(R.id.etUrlImagen);
        etNombre         = findViewById(R.id.etNombreJuego);
        etDescripcion    = findViewById(R.id.etDescripcionJuego);
        etPrecio         = findViewById(R.id.etPrecioJuego);
        etCodigo         = findViewById(R.id.etCodigoJuego);
        tilCodigo        = findViewById(R.id.tilCodigo);
        spinnerGenero    = findViewById(R.id.spinnerGenero);
        spinnerPlataforma= findViewById(R.id.spinnerPlataforma);
        btnBuscarPortada = findViewById(R.id.btnBuscarPortada);
        btnPrevisualizar = findViewById(R.id.btnPrevisualizar);
        btnPublicar      = findViewById(R.id.btnPublicarJuego);
        progressBar      = findViewById(R.id.progressPublicar);
        tvEstadoBusqueda = findViewById(R.id.tvEstadoBusqueda);

        configurarSpinnerPlataforma();
        configurarSpinnerGenero();
        configurarBottomNav();

        btnBuscarPortada.setOnClickListener(v -> {
            String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
            if (nombre.isEmpty()) { etNombre.setError("Escribe el nombre del juego primero"); return; }
            buscarPortadaJuego(nombre);
        });

        btnPrevisualizar.setOnClickListener(v -> {
            String url = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
            if (url.isEmpty()) { etUrl.setError("Escribe una URL"); return; }
            Glide.with(this).load(url).centerCrop()
                    .placeholder(R.mipmap.ic_launcher).error(R.mipmap.ic_launcher).into(ivPreview);
        });

        btnPublicar.setOnClickListener(v -> validarYPublicar());
    }

    // ── Búsqueda de portada (CheapShark → Steam CDN) ──────────────────────

    private void buscarPortadaJuego(String nombre) {
        if (buscando) return;
        buscando = true;
        btnBuscarPortada.setEnabled(false);
        btnBuscarPortada.setText("⏳  Buscando...");
        mostrarEstado("🔍 Buscando \"" + nombre + "\"...", true);

        new Thread(() -> {
            try {
                String query  = URLEncoder.encode(nombre, "UTF-8");
                String apiUrl = String.format(CHEAPSHARK_URL, query);

                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "ChuankGames/1.0");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);

                if (conn.getResponseCode() != 200) {
                    uiHandler.post(() -> onBusquedaFallida("Sin resultados para \"" + nombre + "\""));
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONArray arr = new JSONArray(sb.toString());
                if (arr.length() == 0) {
                    uiHandler.post(() -> onBusquedaFallida("Juego no encontrado. Prueba con otro nombre."));
                    return;
                }

                JSONObject juego   = arr.getJSONObject(0);
                String steamAppId  = juego.optString("steamAppID", "");
                String nombreReal  = juego.optString("external", nombre);

                if (steamAppId.isEmpty()) {
                    uiHandler.post(() -> onBusquedaFallida("Este juego no tiene portada en Steam."));
                    return;
                }

                uiHandler.post(() ->
                        onPortadaEncontrada(String.format(STEAM_HEADER_URL, steamAppId), nombreReal));

            } catch (Exception e) {
                uiHandler.post(() -> onBusquedaFallida("Error de conexión. Revisa internet."));
            }
        }).start();
    }

    private void onPortadaEncontrada(String imageUrl, String nombreJuego) {
        buscando = false;
        btnBuscarPortada.setEnabled(true);
        btnBuscarPortada.setText("🔍  Buscar portada automáticamente");
        etUrl.setText(imageUrl);
        Glide.with(this).load(imageUrl).centerCrop()
                .placeholder(R.mipmap.ic_launcher).error(R.mipmap.ic_launcher).into(ivPreview);
        mostrarEstado("✅ " + nombreJuego, true);
    }

    private void onBusquedaFallida(String msg) {
        buscando = false;
        btnBuscarPortada.setEnabled(true);
        btnBuscarPortada.setText("🔍  Buscar portada automáticamente");
        mostrarEstado("⚠️ " + msg, true);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void mostrarEstado(String texto, boolean visible) {
        tvEstadoBusqueda.setText(texto);
        tvEstadoBusqueda.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    // ── Spinners ───────────────────────────────────────────────────────────

    private void configurarSpinnerPlataforma() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, PLATAFORMAS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPlataforma.setAdapter(adapter);
        spinnerPlataforma.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                plataformaSeleccionada = PLATAFORMAS[pos];
                if (v instanceof TextView)
                    ((TextView) v).setTextColor(getResources().getColor(R.color.white, null));
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void configurarSpinnerGenero() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, GENEROS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGenero.setAdapter(adapter);
        spinnerGenero.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                generoSeleccionado = GENEROS[pos];
                if (v instanceof TextView)
                    ((TextView) v).setTextColor(getResources().getColor(R.color.white, null));
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    // ── Bottom Nav ─────────────────────────────────────────────────────────

    private void configurarBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_añadir);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                Intent i = new Intent(this, PaginaPrincipalActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                return true;
            } else if (id == R.id.nav_buscar) {
                startActivity(new Intent(this, NoticiasActivity.class)); return true;
            } else if (id == R.id.nav_añadir) {
                return true;
            } else if (id == R.id.nav_carrito) {
                startActivity(new Intent(this, BibliotecaActivity.class)); return true;
            } else if (id == R.id.nav_ruleta) {
                startActivity(new Intent(this, RuletaActivity.class)); return true;
            }
            return false;
        });
    }

    // ── Validación ─────────────────────────────────────────────────────────

    private void validarYPublicar() {
        String nombre      = etNombre.getText()      != null ? etNombre.getText().toString().trim()      : "";
        String url         = etUrl.getText()         != null ? etUrl.getText().toString().trim()         : "";
        String descripcion = etDescripcion.getText() != null ? etDescripcion.getText().toString().trim() : "";
        String precioStr   = etPrecio.getText()      != null ? etPrecio.getText().toString().trim()      : "";
        String codigo      = etCodigo.getText()      != null ? etCodigo.getText().toString().trim()      : "";

        if (nombre.isEmpty())      { etNombre.setError("Escribe el nombre del juego");       return; }
        if (url.isEmpty())         { etUrl.setError("Busca o pega la URL de la imagen");     return; }
        if (!url.startsWith("http")){ etUrl.setError("La URL debe empezar por http");        return; }
        if (descripcion.isEmpty()) { etDescripcion.setError("Escribe una descripción");      return; }
        if (precioStr.isEmpty())   { etPrecio.setError("Escribe el precio en €");            return; }
        if (codigo.isEmpty())      { tilCodigo.setError("La clave de activación es obligatoria"); return; }

        double precioEuros;
        try {
            precioEuros = Double.parseDouble(precioStr);
            if (precioEuros <= 0) { etPrecio.setError("El precio debe ser mayor que 0"); return; }
        } catch (NumberFormatException e) {
            etPrecio.setError("Precio inválido"); return;
        }

        tilCodigo.setError(null);
        double precioCK = Math.round(precioEuros * 10.0);
        mostrarCargando(true);
        guardarEnDatabase(nombre, generoSeleccionado, plataformaSeleccionada,
                descripcion, precioCK, url, codigo);
    }

    // ── Guardar en Firebase ────────────────────────────────────────────────

    private void guardarEnDatabase(String nombre, String genero, String plataforma,
                                   String descripcion, double precioCK,
                                   String imagenUrl, String codigo) {
        String uid    = usuario != null ? usuario.getUid() : "anonimo";
        String idJuego= dbJuegos.push().getKey();
        if (idJuego == null) { mostrarCargando(false); return; }

        String nombreVendedor = usuario != null && usuario.getDisplayName() != null
                ? usuario.getDisplayName() : "Usuario";
        long ahora = System.currentTimeMillis();

        Videojuego juego = new Videojuego(idJuego, nombre, genero, descripcion, precioCK, imagenUrl);
        juego.setPublicadoPor(uid);
        juego.setNombreVendedor(nombreVendedor);
        juego.setFechaPublicacion(ahora);
        juego.setDisponible(true);
        juego.setPlataforma(plataforma);
        juego.setCodigoJuego(codigo);   // ← clave real

        dbJuegos.child(idJuego).setValue(juego)
                .addOnSuccessListener(unused -> {
                    java.util.HashMap<String, Object> ev = new java.util.HashMap<>();
                    ev.put("nombre", nombre);
                    ev.put("precio", precioCK);
                    ev.put("imagenUrl", imagenUrl);
                    ev.put("plataforma", plataforma);
                    ev.put("fechaPublicacion", ahora);
                    dbUsuarios.child(uid).child("enVenta").child(idJuego).setValue(ev);

                    mostrarCargando(false);
                    Toast.makeText(this, "¡Juego publicado y en venta!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    mostrarCargando(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void mostrarCargando(boolean on) {
        progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        btnPublicar.setEnabled(!on);
        btnBuscarPortada.setEnabled(!on);
        btnPrevisualizar.setEnabled(!on);
        spinnerGenero.setEnabled(!on);
        spinnerPlataforma.setEnabled(!on);
    }
}
