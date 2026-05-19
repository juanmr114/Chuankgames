package com.example.chuankgames;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

public class DetalleJuegoActivity extends AppCompatActivity {

    public static final String EXTRA_JUEGO_ID = "juego_id";

    // Views
    private ImageButton    btnVolver;
    private TextView       tvHeaderNombre, tvSaldoDetalle;
    private ImageView      ivPortada;
    private TextView       tvNombre, tvGenero, tvVendedor, tvDescripcion;
    private TextView       tvPrecio, tvPrecioEuro, tvCKReward;
    private MaterialButton btnComprarEuro, btnRevender;
    private ProgressBar    progressBar;

    // Firebase
    private DatabaseReference dbJuegos, dbUsuarios;
    private String uid, displayName;

    // State
    private Videojuego juegoActual;
    private double     saldoEur = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_juego);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }
        uid         = user.getUid();
        displayName = user.getDisplayName() != null ? user.getDisplayName() : "Usuario";

        dbJuegos   = FirebaseDatabase.getInstance().getReference("videojuegos");
        dbUsuarios = FirebaseDatabase.getInstance().getReference("usuarios");

        btnVolver      = findViewById(R.id.btnVolverDetalle);
        tvHeaderNombre = findViewById(R.id.tvHeaderNombre);
        tvSaldoDetalle = findViewById(R.id.tvSaldoDetalle);
        ivPortada      = findViewById(R.id.ivDetallePortada);
        tvNombre       = findViewById(R.id.tvDetalleNombre);
        tvGenero       = findViewById(R.id.tvDetalleGenero);
        tvVendedor     = findViewById(R.id.tvDetalleVendedor);
        tvDescripcion  = findViewById(R.id.tvDetalleDescripcion);
        tvPrecio       = findViewById(R.id.tvDetallePrecio);
        tvPrecioEuro   = findViewById(R.id.tvDetallePrecioEuro);
        tvCKReward     = findViewById(R.id.tvCKReward);
        btnComprarEuro = findViewById(R.id.btnComprarEuro);
        btnRevender    = findViewById(R.id.btnRevender);
        progressBar    = findViewById(R.id.progressDetalle);

        btnVolver.setOnClickListener(v -> finish());
        btnComprarEuro.setOnClickListener(v -> confirmarCompraEuro());
        btnRevender.setOnClickListener(v -> confirmarReventa());

        String juegoId = getIntent().getStringExtra(EXTRA_JUEGO_ID);
        if (juegoId == null) { finish(); return; }

        cargarBalances();
        cargarJuego(juegoId);
    }

    // ── Balances ──────────────────────────────────────────────────────────

    private void cargarBalances() {
        dbUsuarios.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                saldoEur = snapshot.child("dinero").exists()
                        ? safeDouble(snapshot.child("dinero")) : 0;
                tvSaldoDetalle.setText(String.format("💶 %.2f€", saldoEur));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private double safeDouble(DataSnapshot ds) {
        try { return ds.getValue(Double.class); } catch (Exception e) { return 0; }
    }

    // ── Carga de juego ────────────────────────────────────────────────────

    private void cargarJuego(String juegoId) {
        progressBar.setVisibility(View.VISIBLE);
        dbJuegos.child(juegoId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                juegoActual = snapshot.getValue(Videojuego.class);
                if (juegoActual == null) { finish(); return; }
                juegoActual.setId(snapshot.getKey());
                mostrarDatos();
                verificarEstado();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { finish(); }
        });
    }

    private void verificarEstado() {
        // El usuario es el publicador (su propio juego)
        if (uid.equals(juegoActual.getPublicadoPor())) {
            btnComprarEuro.setVisibility(View.GONE);
            btnRevender.setVisibility(View.VISIBLE);
            tvCKReward.setVisibility(View.GONE);
            return;
        }
        // Comprobar si ya está en la biblioteca del usuario
        dbUsuarios.child(uid).child("biblioteca").child(juegoActual.getId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) setBought();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ── Mostrar datos ─────────────────────────────────────────────────────

    private void mostrarDatos() {
        String nombre = juegoActual.getNombre() != null ? juegoActual.getNombre() : "";
        tvHeaderNombre.setText(nombre);
        tvNombre.setText(nombre);
        tvGenero.setText("🎮 " + (juegoActual.getGenero() != null ? juegoActual.getGenero() : ""));
        String vendedor = juegoActual.getNombreVendedor() != null
                ? juegoActual.getNombreVendedor() : "Desconocido";
        tvVendedor.setText("👤 " + vendedor);
        tvDescripcion.setText(juegoActual.getDescripcion() != null ? juegoActual.getDescripcion() : "");

        int    ck      = (int) juegoActual.getPrecio();
        double eur     = juegoActual.getPrecioEuros();
        int    ckBonus = Math.max(1, ck / 10);   // 10% del valor en CK

        // Precio principal en € (grande y dorado)
        tvPrecio.setText(String.format("💶 €%.2f", eur));
        tvPrecioEuro.setText("⚡ " + ck + " CK equivalente");
        tvCKReward.setText("🎁 +" + ckBonus + " CK  tú · vendedor");
        tvCKReward.setVisibility(View.VISIBLE);

        actualizarBotones();

        if (juegoActual.getImagenUrl() != null && !juegoActual.getImagenUrl().isEmpty()) {
            Glide.with(this).load(juegoActual.getImagenUrl()).centerCrop().into(ivPortada);
        }

        if (!juegoActual.isDisponible()) {
            btnComprarEuro.setEnabled(false);
            btnComprarEuro.setText("❌ No disponible");
            tvCKReward.setVisibility(View.GONE);
        }
    }

    private void actualizarBotones() {
        if (juegoActual == null) return;
        btnComprarEuro.setText(String.format("🏦 Comprar  €%.2f", juegoActual.getPrecioEuros()));
    }

    private void setBought() {
        btnComprarEuro.setEnabled(false);
        btnComprarEuro.setText("✅ Ya en tu biblioteca");
        tvCKReward.setVisibility(View.GONE);
        btnRevender.setVisibility(View.VISIBLE);
    }

    // ── Compra con € ──────────────────────────────────────────────────────

    private void confirmarCompraEuro() {
        double precioEur = juegoActual.getPrecioEuros();
        int    precioCK  = (int) juegoActual.getPrecio();
        int    ckBonus   = Math.max(1, precioCK / 10);

        if (saldoEur < precioEur) {
            new AlertDialog.Builder(this)
                    .setTitle("❌ Euros insuficientes")
                    .setMessage(String.format(
                            "Necesitas: €%.2f\nTienes:    €%.2f\n\n" +
                            "Añade dinero desde tu 📚 Biblioteca o gana euros en la 🎰 Ruleta.",
                            precioEur, saldoEur))
                    .setPositiveButton("Entendido", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("💳 Confirmar compra")
                .setMessage(
                        "🎮 " + juegoActual.getNombre() + "\n" +
                        "────────────────────────\n" +
                        String.format("Pagarás:       €%.2f\n", precioEur) +
                        "🎁 Tu bonus:   +" + ckBonus + " CK\n" +
                        "🎁 Vendedor:   +" + ckBonus + " CK\n" +
                        "────────────────────────\n" +
                        String.format("Vendedor recibe: €%.2f", precioEur))
                .setPositiveButton("✅ Confirmar",
                        (d, w) -> ejecutarCompraEuro(precioEur, precioCK, ckBonus))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarCompraEuro(double precioEur, int precioCK, int ckBonus) {
        mostrarCargando(true);
        String vendedorId = juegoActual.getPublicadoPor();
        String juegoId    = juegoActual.getId();
        double eurAntes   = saldoEur;

        // 1. Descontar € al comprador
        dbUsuarios.child(uid).child("dinero").runTransaction(new Transaction.Handler() {
            @NonNull @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                Double d = data.getValue(Double.class);
                if (d == null) d = 0.0;
                if (d < precioEur) return Transaction.abort();
                data.setValue(round2(d - precioEur));
                return Transaction.success(data);
            }
            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot snapshot) {
                if (!committed) {
                    mostrarCargando(false);
                    Toast.makeText(DetalleJuegoActivity.this,
                            "Euros insuficientes", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 2. Dar bonus CK al comprador
                dbUsuarios.child(uid).child("saldo").runTransaction(new Transaction.Handler() {
                    @NonNull @Override
                    public Transaction.Result doTransaction(@NonNull MutableData data) {
                        Double s = data.getValue(Double.class);
                        if (s == null) s = 0.0;
                        data.setValue(s + ckBonus);
                        return Transaction.success(data);
                    }
                    @Override
                    public void onComplete(@Nullable DatabaseError e, boolean ok,
                                           @Nullable DataSnapshot snap) {
                        // 3. Pagar € + bonus CK al vendedor, luego registrar compra
                        pagarVendedor(vendedorId, precioEur, ckBonus, () ->
                                registrarCompra(juegoId, precioEur, ckBonus,
                                        vendedorId, eurAntes));
                    }
                });
            }
        });
    }

    // ── Pago al vendedor ──────────────────────────────────────────────────

    interface Callback { void run(); }

    /** Acredita € y bonus CK al vendedor. Siempre paga en euros. */
    private void pagarVendedor(String vendedorId, double precioEur, int ckBonus,
                                Callback callback) {
        if (vendedorId == null || vendedorId.isEmpty() || vendedorId.equals(uid)) {
            callback.run();
            return;
        }
        // Pagar € al vendedor
        dbUsuarios.child(vendedorId).child("dinero").runTransaction(new Transaction.Handler() {
            @NonNull @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                Double v = data.getValue(Double.class);
                if (v == null) v = 0.0;
                data.setValue(round2(v + precioEur));
                return Transaction.success(data);
            }
            @Override
            public void onComplete(@Nullable DatabaseError e, boolean ok,
                                   @Nullable DataSnapshot snap) {
                // Dar bonus CK al vendedor también
                dbUsuarios.child(vendedorId).child("saldo").runTransaction(
                        new Transaction.Handler() {
                    @NonNull @Override
                    public Transaction.Result doTransaction(@NonNull MutableData data) {
                        Double s = data.getValue(Double.class);
                        if (s == null) s = 0.0;
                        data.setValue(s + ckBonus);
                        return Transaction.success(data);
                    }
                    @Override
                    public void onComplete(@Nullable DatabaseError e2, boolean ok2,
                                           @Nullable DataSnapshot snap2) {
                        callback.run();
                    }
                });
            }
        });
    }

    // ── Registrar compra ──────────────────────────────────────────────────

    private void registrarCompra(String juegoId, double precioEur, int ckBonus,
                                  String vendedorId, double eurAntes) {
        // Añadir a biblioteca del comprador
        dbUsuarios.child(uid).child("biblioteca").child(juegoId).setValue(true);

        // Registrar historial
        String compraId = dbUsuarios.child(uid).child("compras").push().getKey();
        if (compraId != null) {
            java.util.HashMap<String, Object> c = new java.util.HashMap<>();
            c.put("juegoId", juegoId);
            c.put("nombre", juegoActual.getNombre());
            c.put("metodoPago", "euro");
            c.put("precioEur", precioEur);
            c.put("precioCK", (int) juegoActual.getPrecio());
            c.put("ckBonus", ckBonus);
            c.put("fecha", System.currentTimeMillis());
            dbUsuarios.child(uid).child("compras").child(compraId).setValue(c);
        }

        // Quitar de la sección enVenta del vendedor
        if (vendedorId != null && !vendedorId.isEmpty()) {
            dbUsuarios.child(vendedorId).child("enVenta").child(juegoId).removeValue();
        }

        // Transferir propiedad al comprador
        java.util.HashMap<String, Object> upd = new java.util.HashMap<>();
        upd.put("disponible", false);
        upd.put("publicadoPor", uid);
        upd.put("nombreVendedor", displayName);
        dbJuegos.child(juegoId).updateChildren(upd);

        mostrarCargando(false);
        mostrarRecibo(precioEur, ckBonus, eurAntes);
    }

    private void mostrarRecibo(double precioEur, int ckBonus, double eurAntes) {
        String resumen = String.format(
                "💳 Pagado:          €%.2f\n" +
                "🎁 Tu bonus CK:    +%d CK\n" +
                "🎁 Bonus vendedor: +%d CK\n" +
                "💶 Euros ahora:    €%.2f",
                precioEur, ckBonus, ckBonus, eurAntes - precioEur);

        new AlertDialog.Builder(this)
                .setTitle("✅ ¡Compra completada!")
                .setMessage("🎮 " + juegoActual.getNombre() + "\n\n" + resumen +
                        "\n\n📚 El juego ya está en tu Biblioteca.")
                .setPositiveButton("¡Genial! 🎉", null)
                .setCancelable(false)
                .show();

        setBought();
    }

    // ── Reventa ───────────────────────────────────────────────────────────

    private void confirmarReventa() {
        double eur     = juegoActual.getPrecioEuros();
        int    ck      = (int) juegoActual.getPrecio();
        int    ckBonus = Math.max(1, ck / 10);

        new AlertDialog.Builder(this)
                .setTitle("🔄 Poner en venta")
                .setMessage("¿Poner \"" + juegoActual.getNombre() + "\" a la venta?\n\n" +
                        String.format(
                                "Recibirás: 💶 €%.2f + 🎁 %d CK de bonus cuando lo compren.\n\n" +
                                "Lo verás en \"Mis juegos en venta\" en tu Biblioteca.",
                                eur, ckBonus))
                .setPositiveButton("Sí, poner en venta", (d, w) -> ejecutarReventa())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarReventa() {
        mostrarCargando(true);
        String juegoId = juegoActual.getId();

        java.util.HashMap<String, Object> updates = new java.util.HashMap<>();
        updates.put("disponible", true);
        updates.put("publicadoPor", uid);
        updates.put("nombreVendedor", displayName);

        dbJuegos.child(juegoId).updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    dbUsuarios.child(uid).child("biblioteca").child(juegoId).removeValue();

                    java.util.HashMap<String, Object> ev = new java.util.HashMap<>();
                    ev.put("nombre", juegoActual.getNombre());
                    ev.put("precio", juegoActual.getPrecio());
                    ev.put("imagenUrl",
                            juegoActual.getImagenUrl() != null ? juegoActual.getImagenUrl() : "");
                    ev.put("pagoPreferido", "euro");
                    ev.put("fechaPublicacion", System.currentTimeMillis());
                    dbUsuarios.child(uid).child("enVenta").child(juegoId).setValue(ev);

                    mostrarCargando(false);
                    new AlertDialog.Builder(DetalleJuegoActivity.this)
                            .setTitle("🔄 ¡Juego en venta!")
                            .setMessage("\"" + juegoActual.getNombre() +
                                    "\" ya está disponible en el mercado.\n\n" +
                                    "Verás tus juegos en venta en tu Biblioteca.")
                            .setPositiveButton("Aceptar", (d, w) -> finish())
                            .show();
                })
                .addOnFailureListener(e -> {
                    mostrarCargando(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void mostrarCargando(boolean on) {
        progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        btnComprarEuro.setEnabled(!on);
        btnRevender.setEnabled(!on);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
