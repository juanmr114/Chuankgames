package com.example.chuankgames;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DetalleJuegoActivity extends AppCompatActivity {

    public static final String EXTRA_JUEGO_ID = "juego_id";

    private ImageView ivPortada;
    private TextView tvNombre, tvGenero, tvVendedor, tvDescripcion, tvPrecio, tvSaldo;
    private MaterialButton btnComprar, btnRevender;
    private ProgressBar progressBar;

    private DatabaseReference dbJuegos, dbUsuarios;
    private String uid;
    private Videojuego juegoActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_juego);

        uid         = FirebaseAuth.getInstance().getCurrentUser().getUid();
        dbJuegos    = FirebaseDatabase.getInstance().getReference("videojuegos");
        dbUsuarios  = FirebaseDatabase.getInstance().getReference("usuarios");

        ivPortada    = findViewById(R.id.ivDetallePortada);
        tvNombre     = findViewById(R.id.tvDetalleNombre);
        tvGenero     = findViewById(R.id.tvDetalleGenero);
        tvVendedor   = findViewById(R.id.tvDetalleVendedor);
        tvDescripcion= findViewById(R.id.tvDetalleDescripcion);
        tvPrecio     = findViewById(R.id.tvDetallePrecio);
        tvSaldo      = findViewById(R.id.tvSaldoDetalle);
        btnComprar   = findViewById(R.id.btnComprar);
        btnRevender  = findViewById(R.id.btnRevender);
        progressBar  = findViewById(R.id.progressDetalle);

        String juegoId = getIntent().getStringExtra(EXTRA_JUEGO_ID);
        if (juegoId == null) { finish(); return; }

        cargarJuego(juegoId);
        cargarSaldoUsuario();

        btnComprar.setOnClickListener(v -> confirmarCompra());
        btnRevender.setOnClickListener(v -> confirmarReventa());
    }

    private void cargarJuego(String juegoId) {
        dbJuegos.child(juegoId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                juegoActual = snapshot.getValue(Videojuego.class);
                if (juegoActual == null) { finish(); return; }
                juegoActual.setId(snapshot.getKey());
                mostrarDatos();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { finish(); }
        });
    }

    private void cargarSaldoUsuario() {
        dbUsuarios.child(uid).child("saldo").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double saldo = snapshot.exists() ? snapshot.getValue(Double.class) : 0;
                tvSaldo.setText("💰 Saldo: " + (int) saldo + " monedas");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void mostrarDatos() {
        tvNombre.setText(juegoActual.getNombre());
        tvGenero.setText("🎮 " + juegoActual.getGenero());
        tvVendedor.setText("👤 Vendido por: " + (juegoActual.getNombreVendedor() != null
                ? juegoActual.getNombreVendedor() : "Desconocido"));
        tvDescripcion.setText(juegoActual.getDescripcion());
        tvPrecio.setText((int) juegoActual.getPrecio() + " monedas");

        if (juegoActual.getImagenUrl() != null && !juegoActual.getImagenUrl().isEmpty()) {
            Glide.with(this).load(juegoActual.getImagenUrl()).centerCrop().into(ivPortada);
        }

        // Es tuyo → mostrar revender en vez de comprar
        if (uid.equals(juegoActual.getPublicadoPor())) {
            btnComprar.setVisibility(View.GONE);
            btnRevender.setVisibility(View.VISIBLE);
        } else if (!juegoActual.isDisponible()) {
            btnComprar.setEnabled(false);
            btnComprar.setText("No disponible");
        }
    }

    private void confirmarCompra() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar compra")
                .setMessage("¿Comprar \"" + juegoActual.getNombre() + "\" por "
                        + (int) juegoActual.getPrecio() + " monedas?")
                .setPositiveButton("Comprar", (d, w) -> ejecutarCompra())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarCompra() {
        mostrarCargando(true);
        String juegoId    = juegoActual.getId();
        String vendedorId = juegoActual.getPublicadoPor();
        double precio     = juegoActual.getPrecio();

        // Transacción atómica: descontar saldo al comprador
        dbUsuarios.child(uid).child("saldo").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                Double saldoActual = data.getValue(Double.class);
                if (saldoActual == null) saldoActual = 0.0;
                if (saldoActual < precio) return Transaction.abort();
                data.setValue(saldoActual - precio);
                return Transaction.success(data);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot snapshot) {
                if (!committed) {
                    mostrarCargando(false);
                    Toast.makeText(DetalleJuegoActivity.this,
                            "Saldo insuficiente", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Sumar saldo al vendedor
                dbUsuarios.child(vendedorId).child("saldo")
                        .runTransaction(new Transaction.Handler() {
                            @NonNull
                            @Override
                            public Transaction.Result doTransaction(@NonNull MutableData data) {
                                Double s = data.getValue(Double.class);
                                if (s == null) s = 0.0;
                                data.setValue(s + precio);
                                return Transaction.success(data);
                            }
                            @Override
                            public void onComplete(@Nullable DatabaseError e, boolean ok,
                                                   @Nullable DataSnapshot snap) {
                                registrarCompraYBiblioteca(juegoId, precio);
                            }
                        });
            }
        });
    }

    private void registrarCompraYBiblioteca(String juegoId, double precio) {
        // Añadir a biblioteca del comprador
        dbUsuarios.child(uid).child("biblioteca").child(juegoId).setValue(true);

        // Registrar en historial de compras
        String compraId = dbUsuarios.child(uid).child("compras").push().getKey();
        if (compraId != null) {
            java.util.HashMap<String, Object> compra = new java.util.HashMap<>();
            compra.put("juegoId", juegoId);
            compra.put("nombre", juegoActual.getNombre());
            compra.put("precio", precio);
            compra.put("fecha", System.currentTimeMillis());
            dbUsuarios.child(uid).child("compras").child(compraId).setValue(compra);
        }

        // Marcar juego como no disponible
        dbJuegos.child(juegoId).child("disponible").setValue(false);
        dbJuegos.child(juegoId).child("publicadoPor").setValue(uid);
        dbJuegos.child(juegoId).child("nombreVendedor")
                .setValue(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());

        mostrarCargando(false);
        Toast.makeText(this, "¡Compra realizada! El juego está en tu biblioteca.", Toast.LENGTH_LONG).show();
        btnComprar.setEnabled(false);
        btnComprar.setText("✅ Comprado");
        btnRevender.setVisibility(View.VISIBLE);
    }

    private void confirmarReventa() {
        new AlertDialog.Builder(this)
                .setTitle("Poner en venta")
                .setMessage("¿Quieres poner \"" + juegoActual.getNombre()
                        + "\" en venta por " + (int) juegoActual.getPrecio() + " monedas?")
                .setPositiveButton("Vender", (d, w) -> ejecutarReventa())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarReventa() {
        mostrarCargando(true);
        String juegoId = juegoActual.getId();

        // Marcar como disponible de nuevo
        dbJuegos.child(juegoId).child("disponible").setValue(true)
                .addOnSuccessListener(unused -> {
                    // Quitar de biblioteca del usuario
                    dbUsuarios.child(uid).child("biblioteca").child(juegoId).removeValue();
                    mostrarCargando(false);
                    Toast.makeText(this, "Juego puesto en venta", Toast.LENGTH_SHORT).show();
                    btnRevender.setVisibility(View.GONE);
                    btnComprar.setVisibility(View.VISIBLE);
                    btnComprar.setEnabled(false);
                    btnComprar.setText("Tu juego en venta");
                })
                .addOnFailureListener(e -> {
                    mostrarCargando(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarCargando(boolean cargando) {
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        btnComprar.setEnabled(!cargando);
        btnRevender.setEnabled(!cargando);
    }
}