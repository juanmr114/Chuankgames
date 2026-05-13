package com.example.chuankgames;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

class PublicarJuegoActivity extends AppCompatActivity {

    private ImageView ivPortada;
    private TextView tvSeleccionarFoto;
    private TextInputEditText etNombre, etGenero, etDescripcion, etPrecio;
    private MaterialButton btnSeleccionarImagen, btnPublicar;
    private ProgressBar progressBar;

    private Uri imagenSeleccionadaUri = null;

    private FirebaseStorage storage;
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;

    // Lanzador para seleccionar imagen de galería
    private final ActivityResultLauncher<String> selectorImagen =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imagenSeleccionadaUri = uri;
                    tvSeleccionarFoto.setVisibility(View.GONE);
                    Glide.with(this).load(uri).centerCrop().into(ivPortada);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publicar_juego);

        mAuth    = FirebaseAuth.getInstance();
        storage  = FirebaseStorage.getInstance();
        dbRef    = FirebaseDatabase.getInstance().getReference("videojuegos");

        ivPortada           = findViewById(R.id.ivPortadaSeleccionada);
        tvSeleccionarFoto   = findViewById(R.id.tvSeleccionarFoto);
        etNombre            = findViewById(R.id.etNombreJuego);
        etGenero            = findViewById(R.id.etGeneroJuego);
        etDescripcion       = findViewById(R.id.etDescripcionJuego);
        etPrecio            = findViewById(R.id.etPrecioJuego);
        btnSeleccionarImagen = findViewById(R.id.btnSeleccionarImagen);
        btnPublicar         = findViewById(R.id.btnPublicarJuego);
        progressBar         = findViewById(R.id.progressPublicar);

        btnSeleccionarImagen.setOnClickListener(v ->
                selectorImagen.launch("image/*"));

        ivPortada.setOnClickListener(v ->
                selectorImagen.launch("image/*"));

        btnPublicar.setOnClickListener(v -> validarYPublicar());
    }

    private void validarYPublicar() {
        String nombre      = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        String genero      = etGenero.getText() != null ? etGenero.getText().toString().trim() : "";
        String descripcion = etDescripcion.getText() != null ? etDescripcion.getText().toString().trim() : "";
        String precioStr   = etPrecio.getText() != null ? etPrecio.getText().toString().trim() : "";

        if (nombre.isEmpty()) { etNombre.setError("Escribe el nombre"); return; }
        if (genero.isEmpty()) { etGenero.setError("Escribe el género"); return; }
        if (descripcion.isEmpty()) { etDescripcion.setError("Escribe una descripción"); return; }
        if (precioStr.isEmpty()) { etPrecio.setError("Escribe el precio"); return; }
        if (imagenSeleccionadaUri == null) {
            Toast.makeText(this, "Selecciona una imagen para el juego", Toast.LENGTH_SHORT).show();
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr);
        } catch (NumberFormatException e) {
            etPrecio.setError("Precio inválido");
            return;
        }

        mostrarCargando(true);
        subirImagenYPublicar(nombre, genero, descripcion, precio);
    }

    private void subirImagenYPublicar(String nombre, String genero, String descripcion, double precio) {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonimo";
        String idJuego = dbRef.push().getKey();
        if (idJuego == null) { mostrarCargando(false); return; }

        // Ruta en Storage: juegos/{idJuego}/portada.jpg
        StorageReference storageRef = storage.getReference()
                .child("juegos")
                .child(idJuego)
                .child("portada.jpg");

        storageRef.putFile(imagenSeleccionadaUri)
                .addOnProgressListener(snapshot -> {
                    // Opcional: mostrar % de subida
                })
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            guardarEnDatabase(idJuego, nombre, genero, descripcion, precio, uri.toString(), uid);
                        })
                )
                .addOnFailureListener(e -> {
                    mostrarCargando(false);
                    Toast.makeText(this, "Error subiendo imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void guardarEnDatabase(String id, String nombre, String genero,
                                   String descripcion, double precio,
                                   String imagenUrl, String uid) {
        Videojuego juego = new Videojuego(id, nombre, genero, descripcion, precio, imagenUrl);
        juego.setPublicadoPor(uid);
        juego.setFechaPublicacion(System.currentTimeMillis());

        dbRef.child(id).setValue(juego)
                .addOnSuccessListener(unused -> {
                    mostrarCargando(false);
                    Toast.makeText(this, "¡Juego publicado!", Toast.LENGTH_SHORT).show();
                    finish(); // Vuelve a la pantalla anterior y recarga
                })
                .addOnFailureListener(e -> {
                    mostrarCargando(false);
                    Toast.makeText(this, "Error guardando datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void mostrarCargando(boolean cargando) {
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnPublicar.setEnabled(!cargando);
        btnSeleccionarImagen.setEnabled(!cargando);
    }
}