package com.example.chuankgames;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PublicarJuegoActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://chuankgames-default-rtdb.europe-west1.firebasedatabase.app";

    private static final String[] GENEROS = {
            "Acción", "Aventura", "RPG", "Deportes", "Simulación",
            "Estrategia", "Puzzle", "Terror", "Multijugador", "Sandbox",
            "Plataformas", "Carreras", "Lucha", "Shooter", "Indie"
    };

    private ImageView         ivPreview;
    private TextInputEditText etUrl, etNombre, etDescripcion, etPrecio;
    private Spinner           spinnerGenero;
    private MaterialButton    btnPrevisualizar, btnPublicar;
    private ProgressBar       progressBar;

    private String generoSeleccionado = GENEROS[0];

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
        spinnerGenero    = findViewById(R.id.spinnerGenero);
        btnPrevisualizar = findViewById(R.id.btnPrevisualizar);
        btnPublicar      = findViewById(R.id.btnPublicarJuego);
        progressBar      = findViewById(R.id.progressPublicar);

        configurarSpinner();
        configurarBottomNav();

        btnPrevisualizar.setOnClickListener(v -> {
            String url = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
            if (url.isEmpty()) { etUrl.setError("Escribe una URL"); return; }
            Glide.with(this)
                    .load(url)
                    .centerCrop()
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(ivPreview);
        });

        btnPublicar.setOnClickListener(v -> validarYPublicar());
    }

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
                startActivity(new Intent(this, NoticiasActivity.class));
                return true;
            } else if (id == R.id.nav_añadir) {
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

    private void configurarSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, GENEROS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGenero.setAdapter(adapter);
        spinnerGenero.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                generoSeleccionado = GENEROS[position];
                if (view instanceof TextView)
                    ((TextView) view).setTextColor(getResources().getColor(R.color.white, null));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void validarYPublicar() {
        String url         = etUrl.getText()         != null ? etUrl.getText().toString().trim()         : "";
        String nombre      = etNombre.getText()       != null ? etNombre.getText().toString().trim()       : "";
        String descripcion = etDescripcion.getText()  != null ? etDescripcion.getText().toString().trim()  : "";
        String precioStr   = etPrecio.getText()       != null ? etPrecio.getText().toString().trim()       : "";

        if (url.isEmpty())             { etUrl.setError("Escribe la URL de la imagen");   return; }
        if (!url.startsWith("http"))   { etUrl.setError("La URL debe empezar por http");  return; }
        if (nombre.isEmpty())          { etNombre.setError("Escribe el nombre");           return; }
        if (descripcion.isEmpty())     { etDescripcion.setError("Escribe una descripción"); return; }
        if (precioStr.isEmpty())       { etPrecio.setError("Escribe el precio en €");     return; }

        double precioEuros;
        try {
            precioEuros = Double.parseDouble(precioStr);
            if (precioEuros <= 0) { etPrecio.setError("El precio debe ser mayor que 0"); return; }
        } catch (NumberFormatException e) {
            etPrecio.setError("Precio inválido");
            return;
        }

        // El campo precio interno (CK) = euros × 10  (10 CK = €1)
        double precioCK = Math.round(precioEuros * 10.0);

        mostrarCargando(true);
        guardarEnDatabase(nombre, generoSeleccionado, descripcion, precioCK, url);
    }

    private void guardarEnDatabase(String nombre, String genero, String descripcion,
                                   double precioCK, String imagenUrl) {
        String uid     = usuario != null ? usuario.getUid() : "anonimo";
        String idJuego = dbJuegos.push().getKey();
        if (idJuego == null) { mostrarCargando(false); return; }

        String nombreVendedor = usuario != null && usuario.getDisplayName() != null
                ? usuario.getDisplayName() : "Usuario";
        long   ahora          = System.currentTimeMillis();

        // Guardar en videojuegos/
        Videojuego juego = new Videojuego(idJuego, nombre, genero, descripcion, precioCK, imagenUrl);
        juego.setPublicadoPor(uid);
        juego.setNombreVendedor(nombreVendedor);
        juego.setFechaPublicacion(ahora);
        juego.setDisponible(true);

        dbJuegos.child(idJuego).setValue(juego)
                .addOnSuccessListener(unused -> {
                    // También añadir a usuarios/{uid}/enVenta para que aparezca en Biblioteca
                    java.util.HashMap<String, Object> ev = new java.util.HashMap<>();
                    ev.put("nombre",           nombre);
                    ev.put("precio",           precioCK);
                    ev.put("imagenUrl",        imagenUrl);
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

    private void mostrarCargando(boolean cargando) {
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnPublicar.setEnabled(!cargando);
        btnPrevisualizar.setEnabled(!cargando);
        spinnerGenero.setEnabled(!cargando);
    }
}
